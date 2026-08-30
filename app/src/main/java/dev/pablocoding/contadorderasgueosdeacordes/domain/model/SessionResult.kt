package dev.pablocoding.contadorderasgueosdeacordes.domain.model

data class SessionResult(
    val id: Long = 0,
    val timestamp: Long,
    val durationSeconds: Int,
    val transitionCount: Int
)
