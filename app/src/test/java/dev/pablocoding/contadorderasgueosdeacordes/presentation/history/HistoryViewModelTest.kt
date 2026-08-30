package dev.pablocoding.contadorderasgueosdeacordes.presentation.history

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.UserPracticeStats
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetPracticeStatsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSessionHistoryUseCase
import dev.pablocoding.contadorderasgueosdeacordes.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getSessionHistoryUseCase: GetSessionHistoryUseCase = mockk()
    private val getPracticeStatsUseCase: GetPracticeStatsUseCase = mockk()

    @Test
    fun `uiState calculates correct stats for empty history`() = runTest {
        every { getSessionHistoryUseCase() } returns flowOf(emptyList())
        every { getPracticeStatsUseCase.calculateStats(emptyList(), any(), any()) } returns UserPracticeStats()

        val viewModel = HistoryViewModel(getSessionHistoryUseCase, getPracticeStatsUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.sessions.size)
            assertEquals(0, state.stats.bestSessionCount)
            assertEquals(0.0, state.stats.averageTransitionsPerSession, 0.001)
            assertEquals(0, state.stats.totalSessions)
            assertEquals(0L, state.stats.totalStrums)
            assertEquals(0L, state.stats.totalPracticeSeconds)
            assertNull(state.bestSessionId)
        }
    }

    @Test
    fun `uiState calculates correct stats for populated session history`() = runTest {
        val sessions = listOf(
            SessionResult(id = 1, timestamp = 1000L, durationSeconds = 60, transitionCount = 40),
            SessionResult(id = 2, timestamp = 2000L, durationSeconds = 60, transitionCount = 60),
            SessionResult(id = 3, timestamp = 3000L, durationSeconds = 60, transitionCount = 50)
        )
        val mockStats = UserPracticeStats(
            totalStrums = 150L,
            totalPracticeSeconds = 180L,
            totalSessions = 3,
            averageTransitionsPerSession = 50.0,
            averageSpeedSpm = 50.0,
            peakSpeedSpm = 60.0,
            bestSessionCount = 60,
            currentStreakDays = 1,
            longestStreakDays = 1
        )
        every { getSessionHistoryUseCase() } returns flowOf(sessions)
        every { getPracticeStatsUseCase.calculateStats(sessions, any(), any()) } returns mockStats

        val viewModel = HistoryViewModel(getSessionHistoryUseCase, getPracticeStatsUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.sessions.size)
            assertEquals(60, state.stats.bestSessionCount)
            assertEquals(50.0, state.stats.averageTransitionsPerSession, 0.001)
            assertEquals(3, state.stats.totalSessions)
            assertEquals(150L, state.stats.totalStrums)
            assertEquals(180L, state.stats.totalPracticeSeconds)
            assertEquals(2L, state.bestSessionId)
        }
    }
}
