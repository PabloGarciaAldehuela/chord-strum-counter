package dev.pablocoding.contadorderasgueosdeacordes.domain.model

data class Session(
    val durationSeconds: Int = 60,
    val transitionCount: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val remainingSeconds: Int = 60,
    val chords: List<String> = listOf("A", "D")
)
