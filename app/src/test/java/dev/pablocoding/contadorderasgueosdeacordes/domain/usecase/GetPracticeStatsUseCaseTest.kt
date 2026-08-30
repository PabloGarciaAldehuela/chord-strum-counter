package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GetPracticeStatsUseCaseTest {

    private lateinit var repository: SessionHistoryRepository
    private lateinit var useCase: GetPracticeStatsUseCase
    private val timeZone = TimeZone.getTimeZone("UTC")

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = GetPracticeStatsUseCase(repository)
    }

    private fun dateToMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar months are 0-indexed
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun `empty history returns default zeroed stats`() = runTest {
        every { repository.getAllSessions() } returns flowOf(emptyList())

        val stats = useCase(timeZone = timeZone).first()

        assertEquals(0L, stats.totalStrums)
        assertEquals(0L, stats.totalPracticeSeconds)
        assertEquals(0, stats.totalSessions)
        assertEquals(0.0, stats.averageTransitionsPerSession, 0.001)
        assertEquals(0.0, stats.averageSpeedSpm, 0.001)
        assertEquals(0.0, stats.peakSpeedSpm, 0.001)
        assertEquals(0, stats.bestSessionCount)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.longestStreakDays)
        assertEquals(0, stats.mostPracticedChords.size)
        assertNull(stats.mostPracticedProgression)
    }

    @Test
    fun `computes lifetime totals and speed accurately`() = runTest {
        val now = dateToMillis(2026, 8, 30)
        val sessions = listOf(
            SessionResult(id = 1, timestamp = now - 3600_000, durationSeconds = 60, transitionCount = 50, chords = listOf("A", "D")),
            SessionResult(id = 2, timestamp = now, durationSeconds = 30, transitionCount = 30, chords = listOf("A", "D", "E")),
            SessionResult(id = 3, timestamp = now + 3600_000, durationSeconds = 120, transitionCount = 80, chords = listOf("C", "G"))
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val stats = useCase(timeZone = timeZone, nowTimestamp = now).first()

        assertEquals(160L, stats.totalStrums) // 50 + 30 + 80
        assertEquals(210L, stats.totalPracticeSeconds) // 60 + 30 + 120
        assertEquals(3, stats.totalSessions)
        assertEquals(160.0 / 3.0, stats.averageTransitionsPerSession, 0.001)
        // Average SPM: (160 strums * 60) / 210s = 45.714 SPM
        assertEquals(45.714, stats.averageSpeedSpm, 0.01)
        // Peak SPM: Session 2 had 30 in 30s = 60 SPM; Session 1 had 50/60 = 50 SPM; Session 3 had 80/120 = 40 SPM -> Peak 60 SPM
        assertEquals(60.0, stats.peakSpeedSpm, 0.001)
        assertEquals(80, stats.bestSessionCount)
    }

    @Test
    fun `calculates chord frequencies and most practiced progression`() = runTest {
        val now = dateToMillis(2026, 8, 30)
        val sessions = listOf(
            SessionResult(id = 1, timestamp = now, durationSeconds = 60, transitionCount = 40, chords = listOf("A", "D")),
            SessionResult(id = 2, timestamp = now, durationSeconds = 60, transitionCount = 42, chords = listOf("A", "D")),
            SessionResult(id = 3, timestamp = now, durationSeconds = 60, transitionCount = 35, chords = listOf("C", "G", "Am"))
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val stats = useCase(timeZone = timeZone, nowTimestamp = now).first()

        // 'A' appeared 2 times, 'D' appeared 2 times, 'C', 'G', 'Am' appeared 1 time
        assertEquals(5, stats.uniqueChordsCount)
        assertEquals("A", stats.mostPracticedChords[0].chordName)
        assertEquals(2, stats.mostPracticedChords[0].count)
        assertEquals("A ➔ D", stats.mostPracticedProgression)
    }

    @Test
    fun `calculates streaks correctly for consecutive days`() = runTest {
        val now = dateToMillis(2026, 8, 30)
        val day1 = dateToMillis(2026, 8, 28)
        val day2 = dateToMillis(2026, 8, 29)
        val day3 = dateToMillis(2026, 8, 30)

        val sessions = listOf(
            SessionResult(id = 1, timestamp = day1, durationSeconds = 60, transitionCount = 40),
            SessionResult(id = 2, timestamp = day2, durationSeconds = 60, transitionCount = 45),
            SessionResult(id = 3, timestamp = day3, durationSeconds = 60, transitionCount = 50)
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val stats = useCase(timeZone = timeZone, nowTimestamp = now).first()

        assertEquals(3, stats.currentStreakDays)
        assertEquals(3, stats.longestStreakDays)
    }

    @Test
    fun `current streak maintained if practiced yesterday but not yet today`() = runTest {
        val now = dateToMillis(2026, 8, 30)
        val day1 = dateToMillis(2026, 8, 28)
        val day2 = dateToMillis(2026, 8, 29)

        val sessions = listOf(
            SessionResult(id = 1, timestamp = day1, durationSeconds = 60, transitionCount = 40),
            SessionResult(id = 2, timestamp = day2, durationSeconds = 60, transitionCount = 45)
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val stats = useCase(timeZone = timeZone, nowTimestamp = now).first()

        assertEquals(2, stats.currentStreakDays)
        assertEquals(2, stats.longestStreakDays)
    }

    @Test
    fun `current streak resets to 0 if last practice was more than 1 day ago`() = runTest {
        val now = dateToMillis(2026, 8, 30)
        val day1 = dateToMillis(2026, 8, 20)
        val day2 = dateToMillis(2026, 8, 21)
        val day3 = dateToMillis(2026, 8, 22)

        val sessions = listOf(
            SessionResult(id = 1, timestamp = day1, durationSeconds = 60, transitionCount = 40),
            SessionResult(id = 2, timestamp = day2, durationSeconds = 60, transitionCount = 45),
            SessionResult(id = 3, timestamp = day3, durationSeconds = 60, transitionCount = 50)
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        val stats = useCase(timeZone = timeZone, nowTimestamp = now).first()

        assertEquals(0, stats.currentStreakDays)
        assertEquals(3, stats.longestStreakDays)
    }

    @Test
    fun `flow calculates stats with dynamic timestamp on emission`() = runTest {
        val day1 = dateToMillis(2026, 8, 29)
        val sessions = listOf(
            SessionResult(id = 1, timestamp = day1, durationSeconds = 60, transitionCount = 40)
        )
        every { repository.getAllSessions() } returns flowOf(sessions)

        // Without fixed nowTimestamp, invoke evaluates System.currentTimeMillis() inside map{}
        val stats = useCase(timeZone = timeZone).first()
        assertEquals(1, stats.totalSessions)
        assertEquals(40L, stats.totalStrums)
    }
}
