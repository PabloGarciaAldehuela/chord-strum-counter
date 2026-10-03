package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerDiagnostics
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerMicrophone
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerSoundEngine
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.di.ApplicationScope
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchTracker
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TunerAnalysisContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** How often to ask the OS whether our capture is being silenced for another app. */
private const val SILENCE_CHECK_INTERVAL_MS = 500L

@Singleton
class TunerRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val microphone: TunerMicrophone,
    private val soundEngine: TunerSoundEngine,
    private val diagnostics: TunerDiagnostics,
    @param:ApplicationScope private val appScope: CoroutineScope
) : TunerRepository {

    private val _settings = MutableStateFlow(TunerSettings())
    override val settings: StateFlow<TunerSettings> = _settings.asStateFlow()

    @Volatile
    private var settingsChangedBeforeLoad = false

    init {
        appScope.launch {
            val stored = preferencesDataSource.getTunerSettings(
                defaultNoteNaming = NoteNaming.defaultForLanguage(Locale.getDefault().language)
            )
            if (!settingsChangedBeforeLoad) _settings.value = stored
        }
    }

    override val isDiagnosticsRecording: StateFlow<Boolean> = diagnostics.isRecording

    override fun pitchSamples(context: StateFlow<TunerAnalysisContext>): Flow<PitchSample> = flow {
        microphone.open().use { input ->
            val capture = diagnostics.startSession(input.sampleRate, input.description)
            try {
                val tracker = PitchTracker(input.sampleRate, context = context.value)
                val buffer = FloatArray(tracker.hopSize)
                val checkIntervalSamples = input.sampleRate * SILENCE_CHECK_INTERVAL_MS / 1000
                var samplesSinceCheck = checkIntervalSamples
                var silenced = false

                while (currentCoroutineContext().isActive) {
                    val read = input.read(buffer, buffer.size)
                    if (read < 0) throw AudioDetectionException.RecordingFailed("Tuner AudioRecord read error: $read")
                    if (read == 0) continue
                    capture?.writeAudio(buffer, read)

                    // Throttled: the check is a binder call.
                    samplesSinceCheck += read
                    if (samplesSinceCheck >= checkIntervalSamples) {
                        samplesSinceCheck = 0
                        silenced = input.isSilenced()
                    }

                    tracker.setContext(context.value)
                    val sample = tracker.process(buffer, read, muted = soundEngine.isDetectionMuted()) ?: continue
                    capture?.writeSample(sample)
                    emit(if (silenced) sample.copy(isInputSilenced = true) else sample)
                }
            } finally {
                capture?.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateSettings(transform: (TunerSettings) -> TunerSettings) {
        settingsChangedBeforeLoad = true
        _settings.update { current ->
            val next = transform(current)
            next.copy(
                referencePitchHz = next.referencePitchHz.coerceIn(
                    TunerSettings.MIN_REFERENCE_PITCH_HZ,
                    TunerSettings.MAX_REFERENCE_PITCH_HZ
                )
            )
        }
        preferencesDataSource.saveTunerSettings(_settings.value)
    }

    override suspend fun playReferenceTone(frequencyHz: Double) = soundEngine.playReferenceTone(frequencyHz)

    override fun playInTuneChime() = soundEngine.playInTuneChime()

    override fun stopSounds() = soundEngine.stop()
}
