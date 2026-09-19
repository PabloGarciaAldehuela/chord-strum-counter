package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

/**
 * Exceptions surfaced during audio recording and acoustic strum detection.
 */
sealed class AudioDetectionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Thrown when audio recording permission (RECORD_AUDIO) has not been granted.
     */
    class PermissionDenied(
        message: String = "Microphone permission is required to detect guitar strums",
        cause: Throwable? = null
    ) : AudioDetectionException(message, cause)

    /**
     * Thrown when audio input hardware cannot be initialized (e.g. mic busy or unsupported audio config).
     */
    class MicrophoneUnavailable(
        message: String = "Microphone hardware is unavailable or in use by another application",
        cause: Throwable? = null
    ) : AudioDetectionException(message, cause)

    /**
     * Thrown when an active AudioRecord session encounters a runtime I/O read failure.
     */
    class RecordingFailed(
        message: String = "Audio recording encountered an unrecoverable failure",
        cause: Throwable? = null
    ) : AudioDetectionException(message, cause)
}
