package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class AudioDetectionMathTest {

    @Test
    fun `calculateThreshold maps sensitivity values correctly`() {
        // Sensitivity 0.0 -> THRESHOLD_LOUD (6000.0)
        assertEquals(6000.0, AudioDetectionMath.calculateThreshold(0.0f), 0.001)

        // Sensitivity 1.0 -> THRESHOLD_QUIET (400.0)
        assertEquals(400.0, AudioDetectionMath.calculateThreshold(1.0f), 0.001)

        // Sensitivity 0.5 -> Midpoint (3200.0)
        assertEquals(3200.0, AudioDetectionMath.calculateThreshold(0.5f), 0.001)

        // Sensitivity 0.6 -> 6000 - 0.6 * 5600 = 6000 - 3360 = 2640.0
        assertEquals(2640.0, AudioDetectionMath.calculateThreshold(0.6f), 0.001)

        // Clamping: below 0 clamps to 0.0, above 1 clamps to 1.0
        assertEquals(6000.0, AudioDetectionMath.calculateThreshold(-0.5f), 0.001)
        assertEquals(400.0, AudioDetectionMath.calculateThreshold(1.5f), 0.001)
    }

    @Test
    fun `calculateRms returns 0 for empty or invalid read count`() {
        val buffer = ShortArray(100) { 1000 }
        assertEquals(0.0, AudioDetectionMath.calculateRms(buffer, 0), 0.001)
        assertEquals(0.0, AudioDetectionMath.calculateRms(buffer, -10), 0.001)
    }

    @Test
    fun `calculateRms returns 0 for all-zero buffer`() {
        val buffer = ShortArray(50) { 0 }
        assertEquals(0.0, AudioDetectionMath.calculateRms(buffer, 50), 0.001)
    }

    @Test
    fun `calculateRms calculates accurate RMS for constant values`() {
        // For constant positive value K, RMS = sqrt(K^2) = K
        val buffer = ShortArray(100) { 500 }
        assertEquals(500.0, AudioDetectionMath.calculateRms(buffer, 100), 0.001)

        // For alternating +K and -K, RMS = sqrt(K^2) = K
        val alternating = ShortArray(100) { if (it % 2 == 0) 1000 else -1000 }
        assertEquals(1000.0, AudioDetectionMath.calculateRms(alternating, 100), 0.001)
    }

    @Test
    fun `calculateRms respects read count smaller than buffer size`() {
        val buffer = ShortArray(10)
        // First 5 elements are 300, remaining 5 are 0
        for (i in 0 until 5) buffer[i] = 300
        // If we only read 5 elements, RMS is 300
        assertEquals(300.0, AudioDetectionMath.calculateRms(buffer, 5), 0.001)
        // If we read all 10, RMS is sqrt((5 * 90000 + 5 * 0) / 10) = sqrt(45000) ~ 212.13
        assertEquals(sqrt(45000.0), AudioDetectionMath.calculateRms(buffer, 10), 0.01)
    }

    @Test
    fun `shouldTriggerStrum triggers only when rms exceeds threshold and debounce time passed`() {
        val threshold = 1000.0
        val debounceMs = 350L
        val lastTime = 1000L

        // Below threshold, outside debounce window -> false
        assertFalse(
            AudioDetectionMath.shouldTriggerStrum(
                rms = 900.0,
                threshold = threshold,
                nowMs = 1500L,
                lastDetectionTimeMs = lastTime,
                debounceMs = debounceMs
            )
        )

        // Above threshold, but inside debounce window -> false
        assertFalse(
            AudioDetectionMath.shouldTriggerStrum(
                rms = 1500.0,
                threshold = threshold,
                nowMs = 1200L, // 200ms < 350ms
                lastDetectionTimeMs = lastTime,
                debounceMs = debounceMs
            )
        )

        // Above threshold, exactly at debounce window boundary -> false (must be strictly > debounceMs)
        assertFalse(
            AudioDetectionMath.shouldTriggerStrum(
                rms = 1500.0,
                threshold = threshold,
                nowMs = 1350L, // 350ms == debounceMs
                lastDetectionTimeMs = lastTime,
                debounceMs = debounceMs
            )
        )

        // Above threshold, outside debounce window -> true
        assertTrue(
            AudioDetectionMath.shouldTriggerStrum(
                rms = 1500.0,
                threshold = threshold,
                nowMs = 1351L, // 351ms > 350ms
                lastDetectionTimeMs = lastTime,
                debounceMs = debounceMs
            )
        )
    }
}
