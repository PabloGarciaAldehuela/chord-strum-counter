package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class StrumDetectorTest {

    @Test
    fun `silence never triggers detection`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val silenceBuffer = ShortArray(1024) { 0 }

        // Process silence at initial time
        val detected = detector.process(silenceBuffer, silenceBuffer.size, nowMs = 1000L)
        assertFalse("Silence buffer must never trigger strum detection", detected)

        // Process silence after time has passed
        val detectedLater = detector.process(silenceBuffer, silenceBuffer.size, nowMs = 2000L)
        assertFalse("Subsequent silence buffer must never trigger strum detection", detectedLater)
    }

    @Test
    fun `a loud buffer above threshold triggers detection`() {
        // Default sensitivity 0.6f maps to threshold = 6000 - 0.6 * (6000 - 400) = 2640.0
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val loudBuffer = ShortArray(512) { 4000 } // RMS = 4000.0 > 2640.0

        val detected = detector.process(loudBuffer, loudBuffer.size, nowMs = 1000L)
        assertTrue("Loud buffer exceeding the threshold must trigger detection", detected)
        assertEquals(1000L, detector.lastDetectionTimeMs)
    }

    @Test
    fun `two loud buffers within the debounce window count only once`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val loudBuffer = ShortArray(512) { 4000 }

        // First loud strum triggers
        val firstTrigger = detector.process(loudBuffer, loudBuffer.size, nowMs = 1000L)
        assertTrue("First loud buffer should trigger detection", firstTrigger)

        // Second loud strum within debounce window (e.g., 200ms < 350ms)
        val secondTrigger = detector.process(loudBuffer, loudBuffer.size, nowMs = 1200L)
        assertFalse("Loud buffer arriving within debounce window must not trigger", secondTrigger)

        // Boundary test: exactly at debounce window (350ms difference, not strictly >)
        val boundaryTrigger = detector.process(loudBuffer, loudBuffer.size, nowMs = 1350L)
        assertFalse("Loud buffer at exact debounce boundary must not trigger", boundaryTrigger)

        // State remains at the timestamp of the first valid trigger
        assertEquals(1000L, detector.lastDetectionTimeMs)
    }

    @Test
    fun `two loud buffers after the debounce window count twice`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val loudBuffer = ShortArray(512) { 4000 }

        // First loud strum at t = 1000L
        val firstTrigger = detector.process(loudBuffer, loudBuffer.size, nowMs = 1000L)
        assertTrue("First loud buffer should trigger detection", firstTrigger)
        assertEquals(1000L, detector.lastDetectionTimeMs)

        // Second loud strum strictly after debounce window (1351L - 1000L = 351ms > 350ms)
        val secondTrigger = detector.process(loudBuffer, loudBuffer.size, nowMs = 1351L)
        assertTrue("Loud buffer after debounce window must trigger detection", secondTrigger)
        assertEquals(1351L, detector.lastDetectionTimeMs)
    }

    @Test
    fun `sensitivity 0_0 and 1_0 map to THRESHOLD_LOUD and THRESHOLD_QUIET`() {
        val detectorMinSensitivity = StrumDetector(sensitivity = 0.0f)
        assertEquals(
            "Sensitivity 0.0 must map to THRESHOLD_LOUD (6000.0)",
            StrumDetector.THRESHOLD_LOUD,
            detectorMinSensitivity.threshold,
            0.001
        )
        assertEquals(6000.0, StrumDetector.calculateThreshold(0.0f), 0.001)

        val detectorMaxSensitivity = StrumDetector(sensitivity = 1.0f)
        assertEquals(
            "Sensitivity 1.0 must map to THRESHOLD_QUIET (400.0)",
            StrumDetector.THRESHOLD_QUIET,
            detectorMaxSensitivity.threshold,
            0.001
        )
        assertEquals(400.0, StrumDetector.calculateThreshold(1.0f), 0.001)
    }

    @Test
    fun `sensitivity outside 0_1 range is clamped`() {
        // Values below 0.0 clamped to 0.0 (THRESHOLD_LOUD)
        val detectorNegative = StrumDetector(sensitivity = -0.5f)
        assertEquals(StrumDetector.THRESHOLD_LOUD, detectorNegative.threshold, 0.001)
        assertEquals(6000.0, StrumDetector.calculateThreshold(-10.0f), 0.001)

        // Values above 1.0 clamped to 1.0 (THRESHOLD_QUIET)
        val detectorExceeding = StrumDetector(sensitivity = 1.75f)
        assertEquals(StrumDetector.THRESHOLD_QUIET, detectorExceeding.threshold, 0.001)
        assertEquals(400.0, StrumDetector.calculateThreshold(99.0f), 0.001)
    }

    @Test
    fun `read count less than or equal to zero never triggers and never divides by zero`() {
        val detector = StrumDetector(sensitivity = 1.0f, debounceMs = 0L)
        val loudBuffer = ShortArray(256) { 30000 }

        // read = 0
        val triggerZeroRead = detector.process(loudBuffer, 0, nowMs = 1000L)
        assertFalse("read = 0 must return false without dividing by zero", triggerZeroRead)
        assertEquals(0.0, StrumDetector.calculateRms(loudBuffer, 0), 0.001)

        // negative read counts
        val triggerNegativeRead1 = detector.process(loudBuffer, -1, nowMs = 1000L)
        assertFalse("Negative read count must return false", triggerNegativeRead1)
        assertEquals(0.0, StrumDetector.calculateRms(loudBuffer, -1), 0.001)

        val triggerNegativeRead100 = detector.process(loudBuffer, -100, nowMs = 1000L)
        assertFalse("Negative read count must return false", triggerNegativeRead100)
        assertEquals(0.0, StrumDetector.calculateRms(loudBuffer, -100), 0.001)

        // Empty buffer
        assertEquals(0.0, StrumDetector.calculateRms(ShortArray(0), 0), 0.001)
    }

    @Test
    fun `reset clears detection timing state`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val loudBuffer = ShortArray(256) { 5000 }

        assertTrue(detector.process(loudBuffer, loudBuffer.size, nowMs = 1000L))
        assertEquals(1000L, detector.lastDetectionTimeMs)

        detector.reset()
        assertEquals(0L, detector.lastDetectionTimeMs)

        // After reset, another strum immediately triggers even if within previous debounce window
        assertTrue(detector.process(loudBuffer, loudBuffer.size, nowMs = 1050L))
        assertEquals(1050L, detector.lastDetectionTimeMs)
    }

    @Test
    fun `rms calculation handles constant, alternating, and partially filled buffers`() {
        // Constant buffer
        val constantBuffer = ShortArray(64) { 800 }
        assertEquals(800.0, StrumDetector.calculateRms(constantBuffer, 64), 0.001)

        // Alternating buffer
        val alternatingBuffer = ShortArray(64) { if (it % 2 == 0) 1200 else -1200 }
        assertEquals(1200.0, StrumDetector.calculateRms(alternatingBuffer, 64), 0.001)

        // Partial read
        val partialBuffer = ShortArray(10)
        for (i in 0 until 5) partialBuffer[i] = 300
        assertEquals(300.0, StrumDetector.calculateRms(partialBuffer, 5), 0.001)
        assertEquals(sqrt(45000.0), StrumDetector.calculateRms(partialBuffer, 10), 0.01)
    }
}
