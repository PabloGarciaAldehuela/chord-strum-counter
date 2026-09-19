package dev.pablocoding.contadorderasgueosdeacordes.presentation.counter

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.UserPracticeStats
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionError
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ClearSessionErrorUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val getPracticeStats: GetPracticeStatsUseCase = mockk(relaxed = true)
    private val toggleMetronome: ToggleMetronomeUseCase = mockk(relaxed = true)
    private val updateMetronomeBpm: UpdateMetronomeBpmUseCase = mockk(relaxed = true)
    private val getSelectedChords: GetSelectedChordsUseCase = mockk(relaxed = true)
    private val updateSelectedChords: UpdateSelectedChordsUseCase = mockk(relaxed = true)
    private val getChordLibrary: GetChordLibraryUseCase = mockk(relaxed = true)
    private val getMetronomeState: GetMetronomeStateUseCase = mockk(relaxed = true)
    private val clearSessionError: ClearSessionErrorUseCase = mockk(relaxed = true)

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
        every { getPracticeStats(any(), any()) } returns flowOf(UserPracticeStats(totalStrums = 120L, currentStreakDays = 2))
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
        getPracticeStats = getPracticeStats,
        toggleMetronome = toggleMetronome,
        updateMetronomeBpm = updateMetronomeBpm,
        getSelectedChords = getSelectedChords,
        updateSelectedChords = updateSelectedChords,
        getChordLibrary = getChordLibrary,
        getMetronomeState = getMetronomeState,
        clearSessionError = clearSessionError
    )

    @Test
    fun `initial uiState combines repository preferences, metronome state and stats`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(60, state.durationSeconds)
            assertEquals(0.6f, state.sensitivity, 0.01f)
            assertEquals(350, state.debounceMs)
            assertEquals(80, state.metronomeBpm)
            assertEquals(false, state.isMetronomePlaying)
            assertEquals(listOf("A", "D"), state.selectedChords)
            assertEquals(120L, state.lifetimeStrums)
            assertEquals(2, state.currentStreakDays)
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

    @Test
    fun `session finish with lower count than previous best sets isPersonalBest to false`() = runTest {
        val pastHistory = listOf(
            SessionResult(id = 1, timestamp = 100L, durationSeconds = 60, transitionCount = 55, chords = listOf("A", "D"))
        )
        every { getSessionHistory() } returns flowOf(pastHistory)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sessionFlow.value = Session(
                durationSeconds = 60,
                transitionCount = 40,
                isRunning = false,
                isFinished = true,
                remainingSeconds = 0,
                chords = listOf("A", "D")
            )

            advanceUntilIdle()

            val finishedState = expectMostRecentItem()
            assertEquals(40, finishedState.transitionCount)
            assertEquals(true, finishedState.isFinished)
            assertFalse(finishedState.isPersonalBest)
        }
    }

    @Test
    fun `session finish calculates personal best strictly for matching progression`() = runTest {
        // High score on [A, D], but we are practicing [C, G] which has no prior history
        val pastHistory = listOf(
            SessionResult(id = 1, timestamp = 100L, durationSeconds = 60, transitionCount = 90, chords = listOf("A", "D"))
        )
        every { getSessionHistory() } returns flowOf(pastHistory)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            sessionFlow.value = Session(
                durationSeconds = 60,
                transitionCount = 20,
                isRunning = false,
                isFinished = true,
                remainingSeconds = 0,
                chords = listOf("C", "G")
            )

            advanceUntilIdle()

            val finishedState = expectMostRecentItem()
            assertEquals(20, finishedState.transitionCount)
            assertEquals(true, finishedState.isFinished)
            // Even though 20 < 90, for [C, G] it is the new PB!
            assertTrue(finishedState.isPersonalBest)
        }
    }

    @Test
    fun `onMetronomeBpmStep clamps at 40 and 240 limits`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()

            // Step down beyond 40
            viewModel.onMetronomeBpmStep(-100)
            coVerify { updateMetronomeBpm(40) }

            // Step up beyond 240
            viewModel.onMetronomeBpmStep(300)
            coVerify { updateMetronomeBpm(240) }
        }
    }

    @Test
    fun `onChordsChange with empty list defaults to A and D`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onChordsChange(listOf("C", "G"))
            val custom = awaitItem()
            assertEquals(listOf("C", "G"), custom.selectedChords)

            viewModel.onChordsChange(emptyList())
            val updated = awaitItem()
            assertEquals(listOf("A", "D"), updated.selectedChords)
        }
        coVerify { updateSelectedChords(listOf("A", "D")) }
    }

    @Test
    fun `getChord delegates to GetChordLibraryUseCase`() {
        val viewModel = createViewModel()
        val mockChord = Chord("Em", "E Minor", listOf(0, 2, 2, 0, 0, 0))
        every { getChordLibrary.getChord("Em") } returns mockChord

        val result = viewModel.getChord("Em")
        assertEquals(mockChord, result)
    }

    @Test
    fun `session error state MicrophonePermissionDenied maps to permission error message in uiState`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val initial = awaitItem()
            assertNull(initial.errorMessage)

            sessionFlow.value = Session(
                isRunning = false,
                error = SessionError.MicrophonePermissionDenied
            )

            val errorState = awaitItem()
            assertEquals("Microphone permission is required to detect guitar strums.", errorState.errorMessage)
            assertFalse(errorState.isRunning)
        }
    }

    @Test
    fun `session error state MicrophoneUnavailable maps to hardware error message in uiState`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()

            sessionFlow.value = Session(
                isRunning = false,
                error = SessionError.MicrophoneUnavailable
            )

            val errorState = awaitItem()
            assertEquals("Microphone hardware is unavailable or in use by another app.", errorState.errorMessage)
        }
    }

    @Test
    fun `session error state RecordingFailed maps to recording error message in uiState`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()

            sessionFlow.value = Session(
                isRunning = false,
                error = SessionError.RecordingFailed
            )

            val errorState = awaitItem()
            assertEquals("Audio recording encountered an error. Please try again.", errorState.errorMessage)
        }
    }

    @Test
    fun `onDismissError delegates to ClearSessionErrorUseCase`() = runTest {
        val viewModel = createViewModel()
        viewModel.onDismissError()
        advanceUntilIdle()

        coVerify(exactly = 1) { clearSessionError() }
    }

    @Test
    fun `onStart clears session error before triggering startSession`() = runTest {
        val viewModel = createViewModel()
        viewModel.onStart()
        advanceUntilIdle()

        coVerify(exactly = 1) { clearSessionError() }
        coVerify(exactly = 1) { startSession(60, listOf("A", "D")) }
    }
}
