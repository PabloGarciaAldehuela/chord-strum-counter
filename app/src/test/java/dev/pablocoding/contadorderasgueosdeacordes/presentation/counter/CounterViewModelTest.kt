package dev.pablocoding.contadorderasgueosdeacordes.presentation.counter

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetChordLibraryUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetMetronomeStateUseCase
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
import dev.pablocoding.contadorderasgueosdeacordes.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val startSession: StartSessionUseCase = mockk(relaxed = true)
    private val stopSession: StopSessionUseCase = mockk(relaxed = true)
    private val updateDuration: UpdateDurationUseCase = mockk(relaxed = true)
    private val updateSensitivity: UpdateSensitivityUseCase = mockk(relaxed = true)
    private val updateDebounce: UpdateDebounceUseCase = mockk(relaxed = true)
    private val saveSessionResult: SaveSessionResultUseCase = mockk(relaxed = true)
    private val getSessionHistory: GetSessionHistoryUseCase = mockk(relaxed = true)
    private val toggleMetronome: ToggleMetronomeUseCase = mockk(relaxed = true)
    private val updateMetronomeBpm: UpdateMetronomeBpmUseCase = mockk(relaxed = true)
    private val getSelectedChords: GetSelectedChordsUseCase = mockk(relaxed = true)
    private val updateSelectedChords: UpdateSelectedChordsUseCase = mockk(relaxed = true)
    private val getChordLibrary: GetChordLibraryUseCase = mockk(relaxed = true)
    private val getMetronomeState: GetMetronomeStateUseCase = mockk(relaxed = true)

    private val sessionFlow = MutableStateFlow(Session())
    private val metronomeStateFlow = MutableStateFlow(MetronomeState(bpm = 80))

    @Before
    fun setUp() {
        every { sessionRepository.sessionFlow } returns sessionFlow
        coEvery { sessionRepository.getPreferredDuration() } returns 60
        coEvery { sessionRepository.getPreferredSensitivity() } returns 0.6f
        coEvery { sessionRepository.getPreferredDebounce() } returns 350
        coEvery { getSelectedChords() } returns listOf("A", "D")
        every { getMetronomeState() } returns metronomeStateFlow
        every { getSessionHistory() } returns flowOf(emptyList())
    }

    private fun createViewModel() = CounterViewModel(
        sessionRepository = sessionRepository,
        startSession = startSession,
        stopSession = stopSession,
        updateDuration = updateDuration,
        updateSensitivity = updateSensitivity,
        updateDebounce = updateDebounce,
        saveSessionResult = saveSessionResult,
        getSessionHistory = getSessionHistory,
        toggleMetronome = toggleMetronome,
        updateMetronomeBpm = updateMetronomeBpm,
        getSelectedChords = getSelectedChords,
        updateSelectedChords = updateSelectedChords,
        getChordLibrary = getChordLibrary,
        getMetronomeState = getMetronomeState
    )

    @Test
    fun `initial uiState combines repository preferences and metronome state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(60, state.durationSeconds)
            assertEquals(0.6f, state.sensitivity, 0.01f)
            assertEquals(350, state.debounceMs)
            assertEquals(80, state.metronomeBpm)
            assertEquals(false, state.isMetronomePlaying)
            assertEquals(listOf("A", "D"), state.selectedChords)
        }
    }

    @Test
    fun `onStart triggers startSession use case with current duration and selected chords`() = runTest {
        val viewModel = createViewModel()
        viewModel.onStart()

        coVerify(exactly = 1) { startSession(60, listOf("A", "D")) }
    }

    @Test
    fun `onStop triggers stopSession use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.onStop()

        coVerify(exactly = 1) { stopSession() }
    }

    @Test
    fun `onDurationChange updates duration state and calls use case`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDurationChange(90)
            val updated = awaitItem()
            assertEquals(90, updated.durationSeconds)
        }
        coVerify { updateDuration(90) }
    }

    @Test
    fun `onSensitivityChange updates sensitivity and calls use case`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onSensitivityChange(0.85f)
            val updated = awaitItem()
            assertEquals(0.85f, updated.sensitivity, 0.01f)
        }
        coVerify { updateSensitivity(0.85f) }
    }

    @Test
    fun `onDebounceChange updates debounce and calls use case`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDebounceChange(500)
            val updated = awaitItem()
            assertEquals(500, updated.debounceMs)
        }
        coVerify { updateDebounce(500) }
    }

    @Test
    fun `onChordsChange updates selected chords state and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onChordsChange(listOf("A", "D", "E"))
            val updated = awaitItem()
            assertEquals(listOf("A", "D", "E"), updated.selectedChords)
        }
        coVerify { updateSelectedChords(listOf("A", "D", "E")) }
    }

    @Test
    fun `onToggleMetronome delegates to toggleMetronome use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.onToggleMetronome(true)

        coVerify { toggleMetronome(true) }
    }

    @Test
    fun `onMetronomeBpmChange delegates to updateMetronomeBpm use case`() = runTest {
        val viewModel = createViewModel()
        viewModel.onMetronomeBpmChange(120)

        coVerify { updateMetronomeBpm(120) }
    }

    @Test
    fun `onMetronomeBpmStep steps BPM up and down correctly`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onMetronomeBpmStep(5)
            coVerify { updateMetronomeBpm(85) }
        }
    }

    @Test
    fun `session finish calculates personal best per chord progression`() = runTest {
        val pastHistory = listOf(
            SessionResult(id = 1, timestamp = 100L, durationSeconds = 60, transitionCount = 55, chords = listOf("A", "D")),
            SessionResult(id = 2, timestamp = 200L, durationSeconds = 60, transitionCount = 30, chords = listOf("A", "D", "E"))
        )
        every { getSessionHistory() } returns flowOf(pastHistory)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            // Session for 3 chords (A, D, E) with 35 transitions.
            // Even though 35 < 55 (the score for A,D), 35 > 30 (the PB for A,D,E), so it SHOULD be a Personal Best!
            sessionFlow.value = Session(
                durationSeconds = 60,
                transitionCount = 35,
                isRunning = false,
                isFinished = true,
                remainingSeconds = 0,
                chords = listOf("A", "D", "E")
            )

            advanceUntilIdle()

            val finishedState = expectMostRecentItem()
            assertEquals(35, finishedState.transitionCount)
            assertEquals(true, finishedState.isFinished)
            assertTrue(finishedState.isPersonalBest)
        }

        coVerify { saveSessionResult(match { it.transitionCount == 35 && it.chords == listOf("A", "D", "E") }) }
    }
}
