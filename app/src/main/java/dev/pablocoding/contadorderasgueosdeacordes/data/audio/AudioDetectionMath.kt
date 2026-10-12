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
     * Legacy signature without onset comparison.
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

    /**
     * Determines whether the current audio frame constitutes a detected strum,
     * including volume onset rise comparison over the pre-onset baseline.
     */
    fun shouldTriggerStrum(
        rms: Double,
        threshold: Double,
        nowMs: Long,
        lastDetectionTimeMs: Long,
        debounceMs: Long,
        baselineRms: Double?,
        onsetRatio: Double = StrumDetector.DEFAULT_ONSET_RATIO
    ): Boolean {
        val isLoudEnough = rms > threshold
        val isDebounced = lastDetectionTimeMs == 0L || (nowMs - lastDetectionTimeMs) > debounceMs
        val isOnset = baselineRms == null || rms >= baselineRms * onsetRatio
        return isLoudEnough && isDebounced && isOnset
    }
}
