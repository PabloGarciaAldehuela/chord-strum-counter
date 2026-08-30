package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordPracticeCount
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.UserPracticeStats
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

internal data class CalendarDay(
    val year: Int,
    val dayOfYear: Int,
    val midnightMillis: Long
) : Comparable<CalendarDay> {
    override fun compareTo(other: CalendarDay): Int = midnightMillis.compareTo(other.midnightMillis)

    fun isNextDay(previous: CalendarDay, timeZone: TimeZone): Boolean {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = previous.midnightMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return this.year == cal.get(Calendar.YEAR) && this.dayOfYear == cal.get(Calendar.DAY_OF_YEAR)
    }
}

class GetPracticeStatsUseCase @Inject constructor(
    private val repository: SessionHistoryRepository
) {

    operator fun invoke(
        timeZone: TimeZone = TimeZone.getDefault(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): Flow<UserPracticeStats> {
        return repository.getAllSessions().map { sessions ->
            calculateStats(sessions, timeZone, nowTimestamp)
        }
    }

    fun calculateStats(
        sessions: List<SessionResult>,
        timeZone: TimeZone = TimeZone.getDefault(),
        nowTimestamp: Long = System.currentTimeMillis()
    ): UserPracticeStats {
        if (sessions.isEmpty()) {
            return UserPracticeStats()
        }

        val totalStrums = sessions.sumOf { it.transitionCount.toLong() }
        val totalPracticeSeconds = sessions.sumOf { it.durationSeconds.toLong() }
        val totalSessions = sessions.size

        val averageTransitionsPerSession = sessions.map { it.transitionCount }.average()
        val averageSpeedSpm = if (totalPracticeSeconds > 0L) {
            (totalStrums.toDouble() * 60.0) / totalPracticeSeconds.toDouble()
        } else {
            0.0
        }

        val peakSpeedSpm = sessions
            .filter { it.durationSeconds > 0 }
            .maxOfOrNull { (it.transitionCount.toDouble() * 60.0) / it.durationSeconds.toDouble() }
            ?: 0.0

        val bestSessionCount = sessions.maxOfOrNull { it.transitionCount } ?: 0

        // Chord frequencies & Repertoire
        val chordFrequencies = sessions
            .flatMap { it.chords }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .map { ChordPracticeCount(it.key, it.value) }
            .sortedWith(compareByDescending<ChordPracticeCount> { it.count }.thenBy { it.chordName })

        val uniqueChordsCount = chordFrequencies.size

        val mostPracticedProgression = sessions
            .map { it.chords.filter { c -> c.isNotBlank() }.joinToString(" ➔ ") }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        // Streaks & Weekly stats calculation
        val today = toCalendarDay(nowTimestamp, timeZone)
        val sessionDays = sessions.map { toCalendarDay(it.timestamp, timeZone) }
        val uniqueDaysAsc = sessionDays.distinct().sorted()

        // Longest Streak
        var longestStreak = 0
        var tempStreak = 0
        var prevDay: CalendarDay? = null

        for (day in uniqueDaysAsc) {
            val previous = prevDay
            if (previous == null || day.isNextDay(previous, timeZone)) {
                tempStreak++
            } else if (day != previous) {
                tempStreak = 1
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDay = day
        }

        // Current Streak
        var currentStreak = 0
        val yesterdayCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = today.midnightMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterday = toCalendarDay(yesterdayCal.timeInMillis, timeZone)

        val lastPracticedDay = uniqueDaysAsc.lastOrNull()
        if (lastPracticedDay != null && (lastPracticedDay == today || lastPracticedDay == yesterday)) {
            var expectedDay: CalendarDay = lastPracticedDay
            val daySet = uniqueDaysAsc.toSet()
            while (daySet.contains(expectedDay)) {
                currentStreak++
                val prevCal = Calendar.getInstance(timeZone).apply {
                    timeInMillis = expectedDay.midnightMillis
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                expectedDay = toCalendarDay(prevCal.timeInMillis, timeZone)
            }
        }

        // Weekly metrics (last 7 days inclusive: today down to today - 6 days)
        val sevenDaysAgoCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = today.midnightMillis
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val minMidnightMillis = sevenDaysAgoCal.timeInMillis
        val maxMidnightMillis = today.midnightMillis

        val weekSessions = sessions.filter {
            val sDay = toCalendarDay(it.timestamp, timeZone)
            sDay.midnightMillis in minMidnightMillis..maxMidnightMillis
        }

        val sessionsThisWeek = weekSessions.size
        val strumsThisWeek = weekSessions.sumOf { it.transitionCount.toLong() }
        val secondsThisWeek = weekSessions.sumOf { it.durationSeconds.toLong() }
        val minutesThisWeek = (secondsThisWeek / 60L).toInt()

        return UserPracticeStats(
            totalStrums = totalStrums,
            totalPracticeSeconds = totalPracticeSeconds,
            totalSessions = totalSessions,
            averageTransitionsPerSession = averageTransitionsPerSession,
            averageSpeedSpm = averageSpeedSpm,
            peakSpeedSpm = peakSpeedSpm,
            bestSessionCount = bestSessionCount,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            sessionsThisWeek = sessionsThisWeek,
            strumsThisWeek = strumsThisWeek,
            minutesThisWeek = minutesThisWeek,
            mostPracticedChords = chordFrequencies,
            uniqueChordsCount = uniqueChordsCount,
            mostPracticedProgression = mostPracticedProgression
        )
    }

    private fun toCalendarDay(timestamp: Long, timeZone: TimeZone): CalendarDay {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            dayOfYear = cal.get(Calendar.DAY_OF_YEAR),
            midnightMillis = cal.timeInMillis
        )
    }
}
