package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.data.db.dao.SessionDao
import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionHistoryRepositoryImplTest {

    private val sessionDao: SessionDao = mockk(relaxed = true)
    private lateinit var repository: SessionHistoryRepositoryImpl

    @Before
    fun setUp() {
        repository = SessionHistoryRepositoryImpl(sessionDao)
    }

    @Test
    fun `getAllSessions maps entities from DAO to domain models`() = runTest {
        val entities = listOf(
            SessionResultEntity(id = 1L, timestamp = 1000L, durationSeconds = 60, transitionCount = 45, chords = "A,D"),
            SessionResultEntity(id = 2L, timestamp = 2000L, durationSeconds = 90, transitionCount = 60, chords = "C,G,Am")
        )
        val daoFlow = MutableStateFlow(entities)
        every { sessionDao.getAllSessions() } returns daoFlow

        repository.getAllSessions().test {
            val sessions = awaitItem()
            assertEquals(2, sessions.size)
            assertEquals(1L, sessions[0].id)
            assertEquals(1000L, sessions[0].timestamp)
            assertEquals(60, sessions[0].durationSeconds)
            assertEquals(45, sessions[0].transitionCount)
            assertEquals(listOf("A", "D"), sessions[0].chords)

            assertEquals(2L, sessions[1].id)
            assertEquals(listOf("C", "G", "Am"), sessions[1].chords)
        }
    }

    @Test
    fun `saveSession maps domain model to entity and inserts into DAO`() = runTest {
        val domain = SessionResult(
            id = 5L,
            timestamp = 123456789L,
            durationSeconds = 120,
            transitionCount = 80,
            chords = listOf("E", "Am", "B7")
        )

        repository.saveSession(domain)

        coVerify(exactly = 1) {
            sessionDao.insertSession(
                match { entity ->
                    entity.id == 5L &&
                    entity.timestamp == 123456789L &&
                    entity.durationSeconds == 120 &&
                    entity.transitionCount == 80 &&
                    entity.chords == "E,Am,B7"
                }
            )
        }
    }

    @Test
    fun `clearAll delegates to sessionDao clearAll`() = runTest {
        repository.clearAll()
        coVerify(exactly = 1) { sessionDao.clearAll() }
    }
}
