package dev.pablocoding.contadorderasgueosdeacordes.presentation.counter

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetMetronomeStateUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSessionHistoryUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.SaveSessionResultUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.StartSessionUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.StopSessionUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ToggleMetronomeUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateDebounceUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateDurationUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateMetronomeBpmUseCase
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
    private val getMetronomeState: GetMetronomeStateUseCase = mockk(relaxed = true)

    private val sessionFlow = MutableStateFlow(Session())
    private val metronomeStateFlow = MutableStateFlow(MetronomeState(bpm = 80))

    @Before
    fun setUp() {
        every { sessionRepository.sessionFlow } returns sessionFlow
        coEvery { sessionRepository.getPreferredDuration() } returns 60
        coEvery { sessionRepository.getPreferredSensitivity() } returns 0.6f
        coEvery { sessionRepository.getPreferredDebounce() } returns 350
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
        }
    }

    @Test
    fun `onStart triggers startSession use case with current duration`() = runTest {
        val viewModel = createViewModel()
        viewModel.onStart()

        coVerify(exactly = 1) { startSession(60) }
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
            awaitItem() // initial state
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
            awaitItem() // initial state
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
            awaitItem() // initial state
            viewModel.onDebounceChange(500)
            val updated = awaitItem()
            assertEquals(500, updated.debounceMs)
        }
        coVerify { updateDebounce(500) }
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
            awaitItem() // initial 80 bpm
            viewModel.onMetronomeBpmStep(5)
            coVerify { updateMetronomeBpm(85) }
        }
    }

    @Test
    fun `session finish saves result and detects personal best`() = runTest {
        val pastHistory = listOf(
            SessionResult(id = 1, timestamp = 100L, durationSeconds = 60, transitionCount = 40)
        )
        every { getSessionHistory() } returns flowOf(pastHistory)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            // Finish session with 50 transitions (> 40 PB)
            sessionFlow.value = Session(
                durationSeconds = 60,
                transitionCount = 50,
                isRunning = false,
                isFinished = true,
                remainingSeconds = 0
            )

            advanceUntilIdle()

            val finishedState = expectMostRecentItem()
            assertEquals(50, finishedState.transitionCount)
            assertEquals(true, finishedState.isFinished)
            assertTrue(finishedState.isPersonalBest)
        }

        coVerify { saveSessionResult(match { it.transitionCount == 50 && it.durationSeconds == 60 }) }
    }
}
