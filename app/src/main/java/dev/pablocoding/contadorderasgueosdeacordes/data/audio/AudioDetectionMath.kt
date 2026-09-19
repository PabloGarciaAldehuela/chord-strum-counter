package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import kotlin.math.sqrt

/**
 * Pure mathematical algorithms for acoustic strum detection.
 * Separated from hardware audio recording APIs for comprehensive unit testing.
 */
object AudioDetectionMath {

    // RMS amplitude thresholds on a 16-bit PCM scale (max = 32767)
    // sensitivity 1.0f = very sensitive (triggers on quiet strums)
    // sensitivity 0.0f = least sensitive (only loud strums trigger)
    const val THRESHOLD_QUIET = 400.0
    const val THRESHOLD_LOUD = 6000.0

    /**
     * Maps sensitivity in [0.0, 1.0] inversely to an amplitude threshold in [THRESHOLD_LOUD, THRESHOLD_QUIET].
     */
    fun calculateThreshold(sensitivity: Float): Double {
        val clamped = sensitivity.coerceIn(0f, 1f)
        return THRESHOLD_LOUD - (clamped * (THRESHOLD_LOUD - THRESHOLD_QUIET))
    }

    /**
     * Calculates the Root Mean Square (RMS) of the PCM short buffer.
     * Returns 0.0 if read count is <= 0.
     */
    fun calculateRms(buffer: ShortArray, read: Int): Double {
        if (read <= 0) return 0.0
        val count = minOf(read, buffer.size)
        var sum = 0.0
        for (i in 0 until count) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / count)
    }

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
