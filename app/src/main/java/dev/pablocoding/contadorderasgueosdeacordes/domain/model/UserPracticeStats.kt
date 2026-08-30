package dev.pablocoding.contadorderasgueosdeacordes.domain.model

data class ChordPracticeCount(
    val chordName: String,
    val count: Int
)

data class UserPracticeStats(
    val totalStrums: Long = 0,
    val totalPracticeSeconds: Long = 0,
    val totalSessions: Int = 0,
    val averageTransitionsPerSession: Double = 0.0,
    val averageSpeedSpm: Double = 0.0,
    val peakSpeedSpm: Double = 0.0,
    val bestSessionCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val sessionsThisWeek: Int = 0,
    val strumsThisWeek: Long = 0,
    val minutesThisWeek: Int = 0,
    val mostPracticedChords: List<ChordPracticeCount> = emptyList(),
    val uniqueChordsCount: Int = 0,
    val mostPracticedProgression: String? = null
)
