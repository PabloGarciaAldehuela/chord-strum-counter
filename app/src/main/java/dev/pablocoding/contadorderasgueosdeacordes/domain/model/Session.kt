package dev.pablocoding.contadorderasgueosdeacordes.domain.model

sealed interface SessionError {
    data object MicrophonePermissionDenied : SessionError
    data object MicrophoneUnavailable : SessionError
    data object RecordingFailed : SessionError
}

data class Session(
    val durationSeconds: Int = 60,
    val transitionCount: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val remainingSeconds: Int = 60,
    val chords: List<String> = listOf("A", "D"),
    val error: SessionError? = null
)
