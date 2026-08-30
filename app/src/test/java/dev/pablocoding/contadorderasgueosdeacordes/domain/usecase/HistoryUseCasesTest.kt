package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HistoryUseCasesTest {

    private lateinit var repository: SessionHistoryRepository
    private lateinit var saveSessionResultUseCase: SaveSessionResultUseCase
    private lateinit var getSessionHistoryUseCase: GetSessionHistoryUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        saveSessionResultUseCase = SaveSessionResultUseCase(repository)
        getSessionHistoryUseCase = GetSessionHistoryUseCase(repository)
    }

    @Test
    fun `SaveSessionResultUseCase invokes repository saveSession`() = runTest {
        val result = SessionResult(id = 1, timestamp = 123456L, durationSeconds = 60, transitionCount = 45)
        saveSessionResultUseCase(result)
        coVerify(exactly = 1) { repository.saveSession(result) }
    }

    @Test
    fun `GetSessionHistoryUseCase returns flow of sessions from repository`() = runTest {
        val sessions = listOf(
            SessionResult(id = 1, timestamp = 123456L, durationSeconds = 60, transitionCount = 45),
            SessionResult(id = 2, timestamp = 123499L, durationSeconds = 60, transitionCount = 52)
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val result = getSessionHistoryUseCase().first()
        assertEquals(2, result.size)
        assertEquals(52, result[1].transitionCount)
    }
}
