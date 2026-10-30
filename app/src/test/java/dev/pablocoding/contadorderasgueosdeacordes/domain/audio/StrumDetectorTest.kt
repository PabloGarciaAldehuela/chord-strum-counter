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

    @Test
    fun `single strong strum with prolonged sustain registers only once`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)

        fun bufferWithRms(targetRms: Short): ShortArray = ShortArray(512) { targetRms }

        var strumCount = 0

        // Silence before strum
        val silence = bufferWithRms(100)
        if (detector.process(silence, silence.size, nowMs = 1000L)) strumCount++

        // Strum attack: huge jump to RMS 18000
        val attack = bufferWithRms(18000)
        if (detector.process(attack, attack.size, nowMs = 1046L)) strumCount++
        assertEquals(1, strumCount)

        // Exponential decay of strings over the next 1.5 seconds (buffers every ~46ms)
        val decayLevels = listOf<Short>(
            15000, 12000, 9500, 7500, 6000, 5000, 4200, // within debounce (< 350ms)
            3600, 3200, 2900, 2700, 2500, 2200, 1800, 1400 // post-debounce sustain (still above threshold 2640 initially!)
        )

        var timeMs = 1046L + 46L
        for (level in decayLevels) {
            val sustainBuffer = bufferWithRms(level)
            if (detector.process(sustainBuffer, sustainBuffer.size, nowMs = timeMs)) {
                strumCount++
            }
            timeMs += 46L
        }

        // Even though post-debounce buffers (e.g. 3600, 3200, 2900, 2700) were > threshold (2640.0),
        // volume-onset detection must recognize they are sustaining and NOT register additional strums.
        assertEquals("Strong strum with long sustain must register exactly once", 1, strumCount)
    }

    @Test
    fun `consecutive strums with intermediate sustain count accurately`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        fun bufferWithRms(targetRms: Short): ShortArray = ShortArray(512) { targetRms }

        var strumCount = 0

        // Strum #1 at t = 1000ms
        if (detector.process(bufferWithRms(18000), 512, nowMs = 1000L)) strumCount++
        assertEquals(1, strumCount)

        // Strings sustain and decay down to 3500 RMS
        val sustainLevels = listOf<Short>(14000, 10000, 7000, 5000, 4200, 3800, 3500)
        var time = 1046L
        for (level in sustainLevels) {
            if (detector.process(bufferWithRms(level), 512, nowMs = time)) strumCount++
            time += 46L
        }
        assertEquals(1, strumCount)

        // Strum #2 strikes at t = 1500ms while strings are still ringing at 3500: volume jumps to 12000
        if (detector.process(bufferWithRms(12000), 512, nowMs = 1500L)) strumCount++
        assertEquals("Subsequent strum on top of ringing strings must trigger Strum #2", 2, strumCount)
    }

    @Test
    fun `softer strum following loud strum is registered`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        fun bufferWithRms(targetRms: Short): ShortArray = ShortArray(512) { targetRms }

        var strumCount = 0

        // Very loud Strum #1
        if (detector.process(bufferWithRms(22000), 512, nowMs = 1000L)) strumCount++
        assertEquals(1, strumCount)

        // Decays to 2800 RMS over 600ms
        var time = 1046L
        for (level in listOf<Short>(15000, 10000, 7000, 5000, 4000, 3200, 2800)) {
            if (detector.process(bufferWithRms(level), 512, nowMs = time)) strumCount++
            time += 46L
        }
        assertEquals(1, strumCount)

        // Strum #2 is gentle (6000 RMS, much softer than Strum #1's 22000, but jumps over 2800 baseline)
        if (detector.process(bufferWithRms(6000), 512, nowMs = 1600L)) strumCount++
        assertEquals("Gentle strum following loud strum must trigger", 2, strumCount)
    }

    @Test
    fun `steady continuous tone never retriggers during sustain`() {
        val detector = StrumDetector(sensitivity = 0.6f, debounceMs = 350L)
        val steadyBuffer = ShortArray(512) { 4000 }

        var strumCount = 0
        // Initial onset from silence triggers strum #1
        if (detector.process(steadyBuffer, 512, nowMs = 1000L)) strumCount++
        assertEquals(1, strumCount)

        // Same continuous tone arrives every 46ms for 1.5 seconds
        for (step in 1..30) {
            val nowMs = 1000L + step * 46L
            if (detector.process(steadyBuffer, 512, nowMs = nowMs)) strumCount++
        }
        assertEquals("Steady continuous signal must not trigger repeatedly after debounce", 1, strumCount)
    }

    @Test
    fun `high sensitivity setting does not cause sustain runaway`() {
        // High sensitivity: threshold = 400.0
        val detector = StrumDetector(sensitivity = 1.0f, debounceMs = 350L)
        fun bufferWithRms(targetRms: Short): ShortArray = ShortArray(512) { targetRms }

        var strumCount = 0
        // Loud strum at t = 1000ms
        if (detector.process(bufferWithRms(16000), 512, nowMs = 1000L)) strumCount++
        assertEquals(1, strumCount)

        // Strings sustain at levels well above 400.0 (e.g. 2000, 1500, 900, 600)
        val longDecay = listOf<Short>(
            12000, 9000, 7000, 5000, 4000, 3000, 2500, // < 350ms
            2000, 1700, 1500, 1300, 1100, 900, 700, 600 // > 350ms, all > 400
        )
        var time = 1046L
        for (level in longDecay) {
            if (detector.process(bufferWithRms(level), 512, nowMs = time)) strumCount++
            time += 46L
        }
        assertEquals("High sensitivity must not retrigger on decaying sustain", 1, strumCount)
    }
}
