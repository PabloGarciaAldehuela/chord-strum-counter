package dev.pablocoding.contadorderasgueosdeacordes.presentation.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetChordLibraryUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetMetronomeStateUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetPracticeStatsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSelectedChordsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSessionHistoryUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.SaveSessionResultUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.StartSessionUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.StopSessionUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ToggleMetronomeUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateDebounceUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateDurationUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateMetronomeBpmUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateSelectedChordsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateSensitivityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CounterUiState(
    val transitionCount: Int = 0,
    val remainingSeconds: Int = 60,
    val durationSeconds: Int = 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isPersonalBest: Boolean = false,
    val sensitivity: Float = 0.6f,
    val debounceMs: Int = 350,
    val isMetronomePlaying: Boolean = false,
    val metronomeBpm: Int = 80,
    val metronomeBeat: Int = 1,
    val metronomeTempoName: String = "Andante",
    val selectedChords: List<String> = listOf("A", "D"),
    val lifetimeStrums: Long = 0,
    val currentStreakDays: Int = 0
)

@HiltViewModel
class CounterViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val startSession: StartSessionUseCase,
    private val stopSession: StopSessionUseCase,
    private val updateDuration: UpdateDurationUseCase,
    private val updateSensitivity: UpdateSensitivityUseCase,
    private val updateDebounce: UpdateDebounceUseCase,
    private val saveSessionResult: SaveSessionResultUseCase,
    private val getSessionHistory: GetSessionHistoryUseCase,
    private val getPracticeStats: GetPracticeStatsUseCase,
    private val toggleMetronome: ToggleMetronomeUseCase,
    private val updateMetronomeBpm: UpdateMetronomeBpmUseCase,
    private val getSelectedChords: GetSelectedChordsUseCase,
    private val updateSelectedChords: UpdateSelectedChordsUseCase,
    private val getChordLibrary: GetChordLibraryUseCase,
    getMetronomeState: GetMetronomeStateUseCase
) : ViewModel() {

    private val _durationSeconds = MutableStateFlow(60)
    private val _sensitivity     = MutableStateFlow(0.6f)
    private val _debounceMs      = MutableStateFlow(350)
    private val _selectedChords  = MutableStateFlow(listOf("A", "D"))
    private val _isPersonalBest  = MutableStateFlow(false)

    private data class Settings(
        val duration: Int,
        val sensitivity: Float,
        val debounceMs: Int,
        val selectedChords: List<String>,
        val isPersonalBest: Boolean
    )

    private val _settings = combine(
        _durationSeconds, _sensitivity, _debounceMs, _selectedChords, _isPersonalBest
    ) { d, s, db, chords, pb -> Settings(d, s, db, chords, pb) }

    val uiState: StateFlow<CounterUiState> = combine(
        sessionRepository.sessionFlow,
        _settings,
        getMetronomeState(),
        getPracticeStats()
    ) { session, settings, metronome, stats ->
        CounterUiState(
            transitionCount = session.transitionCount,
            remainingSeconds = session.remainingSeconds,
            durationSeconds = if (session.isRunning || session.isFinished) session.durationSeconds else settings.duration,
            isRunning = session.isRunning,
            isFinished = session.isFinished,
            isPersonalBest = settings.isPersonalBest,
            sensitivity = settings.sensitivity,
            debounceMs = settings.debounceMs,
            isMetronomePlaying = metronome.isPlaying,
            metronomeBpm = metronome.bpm,
            metronomeBeat = metronome.currentBeat,
            metronomeTempoName = metronome.tempoName,
            selectedChords = if (session.isRunning || session.isFinished) session.chords else settings.selectedChords,
            lifetimeStrums = stats.totalStrums,
            currentStreakDays = stats.currentStreakDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CounterUiState()
    )

    init {
        viewModelScope.launch {
            _durationSeconds.value = sessionRepository.getPreferredDuration()
            _sensitivity.value     = sessionRepository.getPreferredSensitivity()
            _debounceMs.value      = sessionRepository.getPreferredDebounce()
            _selectedChords.value  = getSelectedChords()
        }
        viewModelScope.launch {
            sessionRepository.sessionFlow.collect { session ->
                if (session.isFinished) onSessionFinished(session)
            }
        }
    }

    fun onStart() {
        viewModelScope.launch {
            _isPersonalBest.value = false
            startSession(_durationSeconds.value, _selectedChords.value)
        }
    }

    fun onStop() {
        viewModelScope.launch { stopSession() }
    }

    fun onDurationChange(seconds: Int) {
        viewModelScope.launch {
            updateDuration(seconds)
            _durationSeconds.value = seconds.coerceIn(15, 300)
        }
    }

    fun onSensitivityChange(value: Float) {
        viewModelScope.launch {
            updateSensitivity(value)
            _sensitivity.value = value.coerceIn(0f, 1f)
        }
    }

    fun onDebounceChange(ms: Int) {
        viewModelScope.launch {
            updateDebounce(ms)
            _debounceMs.value = ms.coerceIn(100, 800)
        }
    }

    fun onChordsChange(chords: List<String>) {
        val valid = if (chords.isEmpty()) listOf("A", "D") else chords
        viewModelScope.launch {
            updateSelectedChords(valid)
            _selectedChords.value = valid
        }
    }

    fun onToggleMetronome(enabled: Boolean? = null) {
        viewModelScope.launch {
            toggleMetronome(enabled)
        }
    }

    fun onMetronomeBpmChange(bpm: Int) {
        viewModelScope.launch {
            updateMetronomeBpm(bpm)
        }
    }

    fun onMetronomeBpmStep(delta: Int) {
        val current = uiState.value.metronomeBpm
        onMetronomeBpmChange((current + delta).coerceIn(40, 240))
    }

    fun getChord(name: String): Chord? = getChordLibrary.getChord(name)

    private suspend fun onSessionFinished(session: Session) {
        val result = SessionResult(
            timestamp = System.currentTimeMillis(),
            durationSeconds = session.durationSeconds,
            transitionCount = session.transitionCount,
            chords = session.chords
        )
        saveSessionResult(result)

        // Motivation protection: Personal Best is computed per chord progression
        val currentProgressionKey = session.chords.joinToString(",")
        val history = getSessionHistory().first()
        val best = history
            .filter {
                it.durationSeconds == session.durationSeconds &&
                it.chords.joinToString(",") == currentProgressionKey
            }
            .maxOfOrNull { it.transitionCount } ?: 0
        _isPersonalBest.value = session.transitionCount >= best
    }
}
