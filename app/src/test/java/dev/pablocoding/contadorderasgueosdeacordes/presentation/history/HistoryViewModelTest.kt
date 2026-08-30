package dev.pablocoding.contadorderasgueosdeacordes.presentation.history

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
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

    @Test
    fun `uiState calculates correct stats for empty history`() = runTest {
        every { getSessionHistoryUseCase() } returns flowOf(emptyList())

        val viewModel = HistoryViewModel(getSessionHistoryUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.sessions.size)
            assertEquals(0, state.stats.bestCount)
            assertEquals(0.0, state.stats.averageCount, 0.001)
            assertEquals(0, state.stats.totalSessions)
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
        every { getSessionHistoryUseCase() } returns flowOf(sessions)

        val viewModel = HistoryViewModel(getSessionHistoryUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.sessions.size)
            assertEquals(60, state.stats.bestCount)
            assertEquals(50.0, state.stats.averageCount, 0.001)
            assertEquals(3, state.stats.totalSessions)
            assertEquals(2L, state.bestSessionId)
        }
    }
}
