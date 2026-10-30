package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PracticeSessionActiveException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TunerAnalysisContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TuningEvaluator
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerError
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerTolerance
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetMetronomeStateUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetTunerSettingsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetTuningsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ObserveTunerDiagnosticsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ObserveTunerPitchUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.PlayTunerSoundUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ToggleMetronomeUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateTunerSettingsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TunerUiState(
    val tuning: Tuning,
    val settings: TunerSettings = TunerSettings(),
    val isAutoMode: Boolean = true,
    val lockedStringIndex: Int? = null,
    val reading: TunerReading = TunerReading(),
    val tunedStrings: Set<Int> = emptySet(),
    /** Increments every time a string reaches pitch; drives haptics and the bubble pop. */
    val inTuneEventCount: Int = 0,
    val hasMicPermission: Boolean = false,
    val error: TunerError? = null,
    val showMetronomePausedMessage: Boolean = false,
    /** A developer diagnostics capture is writing the microphone to disk (debug builds only). */
    val isDiagnosticsRecording: Boolean = false
) {
    val allStringsTuned: Boolean
        get() = !tuning.isChromatic && tunedStrings.size == tuning.strings.size

    /** String highlighted on the headstock: the locked one, else the auto-matched one while sounding. */
    val activeStringIndex: Int?
        get() = lockedStringIndex ?: reading.targetStringIndex?.takeIf { reading.hasPitch }
}

private data class TunerControls(
    val isAutoMode: Boolean = true,
    val lockedStringIndex: Int? = null,
    val tunedStrings: Set<Int> = emptySet(),
    val inTuneEventCount: Int = 0,
    val hasMicPermission: Boolean = false,
    val error: TunerError? = null,
    val showMetronomePausedMessage: Boolean = false
)

/** Keeps the mic open briefly across configuration changes, but releases it quickly in background. */
private const val STOP_TIMEOUT_MS = 300L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val observeTunerPitch: ObserveTunerPitchUseCase,
    getTunerSettings: GetTunerSettingsUseCase,
    private val updateTunerSettings: UpdateTunerSettingsUseCase,
    private val getTunings: GetTuningsUseCase,
    private val playTunerSound: PlayTunerSoundUseCase,
    private val getMetronomeState: GetMetronomeStateUseCase,
    private val toggleMetronome: ToggleMetronomeUseCase,
    observeTunerDiagnostics: ObserveTunerDiagnosticsUseCase
) : ViewModel() {

    val tunings: List<Tuning> = getTunings()

    private val settings: StateFlow<TunerSettings> = getTunerSettings()
    private val controls = MutableStateFlow(TunerControls())
    private val restartSignal = MutableStateFlow(0)

    // Only touched from the main thread (the reading pipeline is collected in viewModelScope).
    private val evaluator = TuningEvaluator()

    /** What the audio pipeline listens for; read by the capture thread on every hop. */
    private val analysisContext: StateFlow<TunerAnalysisContext> =
        combine(settings, controls.map { it.lockedStringIndex }.distinctUntilChanged()) { s, locked ->
            analysisContextFor(s, locked)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, analysisContextFor(settings.value, null))

    private fun analysisContextFor(s: TunerSettings, locked: Int?): TunerAnalysisContext {
        val tuning = getTunings.getTuning(s.tuningId)
        return TunerAnalysisContext(
            targetsHz = tuning.strings.map { PitchMath.midiToFrequency(it, s.referencePitchHz.toDouble()) },
            lockedIndex = locked?.takeIf { it in tuning.strings.indices },
            readingMode = s.readingMode
        )
    }

    private val readings: Flow<TunerReading> =
        combine(controls.map { it.hasMicPermission }.distinctUntilChanged(), restartSignal) { granted, _ -> granted }
            .flatMapLatest { granted ->
                if (!granted) {
                    flowOf(TunerReading())
                } else {
                    observeTunerPitch(analysisContext)
                        .map(::evaluate)
                        .onStart {
                            evaluator.reset()
                            emit(TunerReading())
                        }
                        .catch { e ->
                            onPitchError(e)
                            emit(TunerReading())
                        }
                }
            }

    val uiState: StateFlow<TunerUiState> = combine(
        readings, settings, controls, observeTunerDiagnostics()
    ) { reading, s, c, recording ->
        TunerUiState(
            tuning = getTunings.getTuning(s.tuningId),
            settings = s,
            isAutoMode = c.isAutoMode,
            lockedStringIndex = c.lockedStringIndex,
            reading = reading,
            tunedStrings = c.tunedStrings,
            inTuneEventCount = c.inTuneEventCount,
            hasMicPermission = c.hasMicPermission,
            error = c.error,
            showMetronomePausedMessage = c.showMetronomePausedMessage,
            isDiagnosticsRecording = recording
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = TunerUiState(tuning = getTunings.getTuning(settings.value.tuningId), settings = settings.value)
    )

    init {
        // Metronome clicks would pollute pitch detection.
        viewModelScope.launch {
            if (getMetronomeState().value.isPlaying) {
                toggleMetronome(false)
                controls.update { it.copy(showMetronomePausedMessage = true) }
            }
        }
        // New targets invalidate tuned strings and in-tune timers.
        settings
            .map { it.tuningId to it.referencePitchHz }
            .distinctUntilChanged()
            .drop(1)
            .onEach {
                evaluator.reset()
                controls.update { it.copy(isAutoMode = true, lockedStringIndex = null, tunedStrings = emptySet()) }
            }
            .launchIn(viewModelScope)
    }

    private fun evaluate(sample: PitchSample): TunerReading {
        val s = settings.value
        val c = controls.value
        val reading = evaluator.evaluate(
            sample = sample,
            tuning = getTunings.getTuning(s.tuningId),
            referencePitchHz = s.referencePitchHz.toDouble(),
            lockedStringIndex = c.lockedStringIndex,
            toleranceCents = s.tolerance.cents
        )
        if (reading.justBecameInTune) {
            controls.update { current ->
                current.copy(
                    tunedStrings = reading.targetStringIndex?.let { current.tunedStrings + it } ?: current.tunedStrings,
                    inTuneEventCount = current.inTuneEventCount + 1
                )
            }
            if (s.inTuneSound) playTunerSound.inTuneChime()
        }
        return reading
    }

    private fun onPitchError(e: Throwable) {
        val error = when (e) {
            is AudioDetectionException.PermissionDenied -> TunerError.MicrophonePermissionDenied
            is AudioDetectionException.MicrophoneUnavailable -> TunerError.MicrophoneUnavailable
            is PracticeSessionActiveException -> TunerError.PracticeSessionActive
            else -> TunerError.RecordingFailed
        }
        controls.update {
            it.copy(
                error = error,
                hasMicPermission = if (error == TunerError.MicrophonePermissionDenied) false else it.hasMicPermission
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        controls.update {
            it.copy(
                hasMicPermission = granted,
                error = if (granted && it.error == TunerError.MicrophonePermissionDenied) null else it.error
            )
        }
    }

    fun onRetry() {
        controls.update { it.copy(error = null) }
        restartSignal.update { it + 1 }
    }

    fun onDismissError() {
        controls.update { it.copy(error = null) }
    }

    fun onAutoModeChange(enabled: Boolean) {
        evaluator.reset()
        val currentTarget = uiState.value.reading.targetStringIndex
        controls.update {
            if (enabled) it.copy(isAutoMode = true, lockedStringIndex = null)
            else it.copy(isAutoMode = false, lockedStringIndex = currentTarget ?: it.lockedStringIndex ?: 0)
        }
    }

    /** Manual mode: lock the tuner to this string and play its reference tone. */
    fun onStringTapped(index: Int) {
        val s = settings.value
        val tuning = getTunings.getTuning(s.tuningId)
        if (index !in tuning.strings.indices) return
        evaluator.reset()
        controls.update { it.copy(isAutoMode = false, lockedStringIndex = index) }
        viewModelScope.launch {
            playTunerSound.referenceTone(PitchMath.midiToFrequency(tuning.strings[index], s.referencePitchHz.toDouble()))
        }
    }

    fun onSelectTuning(id: String) = updateSettings { it.copy(tuningId = id) }

    fun onReferencePitchChange(hz: Int) = updateSettings {
        it.copy(referencePitchHz = hz.coerceIn(TunerSettings.MIN_REFERENCE_PITCH_HZ, TunerSettings.MAX_REFERENCE_PITCH_HZ))
    }

    fun onInTuneSoundChange(enabled: Boolean) = updateSettings { it.copy(inTuneSound = enabled) }

    fun onLeftHandedChange(enabled: Boolean) = updateSettings { it.copy(leftHanded = enabled) }

    fun onNoteNamingChange(naming: NoteNaming) = updateSettings { it.copy(noteNaming = naming) }

    fun onToleranceChange(tolerance: TunerTolerance) = updateSettings { it.copy(tolerance = tolerance) }

    fun onReadingModeChange(mode: TunerReadingMode) = updateSettings { it.copy(readingMode = mode) }

    fun onMetronomeMessageShown() {
        controls.update { it.copy(showMetronomePausedMessage = false) }
    }

    private fun updateSettings(transform: (TunerSettings) -> TunerSettings) {
        viewModelScope.launch { updateTunerSettings(transform) }
    }

    override fun onCleared() {
        playTunerSound.stop()
    }
}
