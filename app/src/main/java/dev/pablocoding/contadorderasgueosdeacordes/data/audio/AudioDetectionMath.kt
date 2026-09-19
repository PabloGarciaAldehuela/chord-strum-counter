package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.StrumDetector

/**
 * Pure mathematical algorithms for acoustic strum detection.
 * Delegates directly to [StrumDetector] to maintain backward compatibility.
 */
object AudioDetectionMath {

    const val THRESHOLD_QUIET = StrumDetector.THRESHOLD_QUIET
    const val THRESHOLD_LOUD = StrumDetector.THRESHOLD_LOUD

    /**
     * Maps sensitivity in [0.0, 1.0] inversely to an amplitude threshold in [THRESHOLD_LOUD, THRESHOLD_QUIET].
     */
    fun calculateThreshold(sensitivity: Float): Double =
        StrumDetector.calculateThreshold(sensitivity)

    /**
     * Calculates the Root Mean Square (RMS) of the PCM short buffer.
     * Returns 0.0 if read count is <= 0.
     */
    fun calculateRms(buffer: ShortArray, read: Int): Double =
        StrumDetector.calculateRms(buffer, read)

    /**
     * Determines whether the current audio frame constitutes a detected strum.
     */
    fun shouldTriggerStrum(
        rms: Double,
        threshold: Double,
        nowMs: Long,
        lastDetectionTimeMs: Long,
        debounceMs: Long
    ): Boolean {
        return rms > threshold && (nowMs - lastDetectionTimeMs) > debounceMs
    }
}
