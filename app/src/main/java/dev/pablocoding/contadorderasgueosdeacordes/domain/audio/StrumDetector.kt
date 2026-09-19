package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.sqrt

/**
 * Pure, platform-independent detector for acoustic guitar strums from raw 16-bit PCM audio.
 *
 * Encapsulates:
 * 1. Root Mean Square (RMS) amplitude calculation over audio buffers.
 * 2. Sensitivity-to-threshold mapping with clamping.
 * 3. Time-based debounce logic for refractory separation of strums.
 */
class StrumDetector(
    val sensitivity: Float = DEFAULT_SENSITIVITY,
    val debounceMs: Long = DEFAULT_DEBOUNCE_MS
) {
    companion object {
        const val DEFAULT_SENSITIVITY = 0.6f
        const val DEFAULT_DEBOUNCE_MS = 350L

        // RMS amplitude thresholds on a 16-bit PCM scale (max = 32767)
        // sensitivity 1.0f = very sensitive (triggers on quiet strums)
        // sensitivity 0.0f = least sensitive (only loud strums trigger)
        const val THRESHOLD_QUIET = 400.0   // high sensitivity end
        const val THRESHOLD_LOUD  = 6000.0  // low sensitivity end

        /**
         * Maps sensitivity [0.0, 1.0] inversely to an amplitude threshold in [THRESHOLD_LOUD, THRESHOLD_QUIET].
         * Sensitivity values outside [0.0, 1.0] are clamped to bounds.
         */
        fun calculateThreshold(sensitivity: Float): Double {
            val clamped = sensitivity.coerceIn(0f, 1f)
            return THRESHOLD_LOUD - (clamped * (THRESHOLD_LOUD - THRESHOLD_QUIET))
        }

        /**
         * Calculates Root Mean Square (RMS) over a ShortArray buffer and read count.
         * Returns 0.0 if read count is <= 0 or buffer is empty. Never divides by zero.
         */
        fun calculateRms(buffer: ShortArray, read: Int): Double {
            if (read <= 0 || buffer.isEmpty()) return 0.0
            val count = minOf(read, buffer.size)
            if (count <= 0) return 0.0
            var sum = 0.0
            for (i in 0 until count) {
                val sample = buffer[i].toDouble()
                sum += sample * sample
            }
            return sqrt(sum / count)
        }
    }

    /**
     * The computed threshold based on the configured sensitivity.
     */
    val threshold: Double = calculateThreshold(sensitivity)

    /**
     * Monotonic timestamp of the most recent detected strum, in milliseconds.
     */
    var lastDetectionTimeMs: Long = 0L
        private set

    /**
     * Processes an audio buffer frame and evaluates whether a strum occurred.
     *
     * @param buffer Raw 16-bit PCM audio samples.
     * @param read Number of valid samples read in the buffer.
     * @param nowMs Monotonic timestamp in milliseconds.
     * @return True if a strum was detected and debounced; false otherwise.
     */
    fun process(buffer: ShortArray, read: Int, nowMs: Long): Boolean {
        if (read <= 0) return false
        val rms = calculateRms(buffer, read)
        if (rms > threshold && (nowMs - lastDetectionTimeMs) > debounceMs) {
            lastDetectionTimeMs = nowMs
            return true
        }
        return false
    }

    /**
     * Resets detection state (e.g. at the start of a new practice session).
     */
    fun reset() {
        lastDetectionTimeMs = 0L
    }
}
