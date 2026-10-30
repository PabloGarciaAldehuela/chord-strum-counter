package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MpmPitchDetectorTest {

    // Every open-string frequency used by the tuning library (A1 = 55 Hz up to E4 = 329.63 Hz)
    // plus a few higher notes for fretted/chromatic use.
    private val testFrequencies = listOf(
        55.0, 61.74, 65.41, 69.30, 73.42, 77.78, 82.41, 92.50, 98.0, 103.83, 110.0, 123.47,
        130.81, 138.59, 146.83, 164.81, 174.61, 185.0, 196.0, 207.65, 220.0, 233.08, 246.94,
        261.63, 277.18, 293.66, 311.13, 329.63, 440.0, 659.26, 987.77
    )

    private fun detector(sampleRate: Int) = MpmPitchDetector(sampleRate, windowSize = 4096)

    @Test
    fun `pure sines are detected within half a cent at 48 kHz and 44_1 kHz`() {
        for (sampleRate in listOf(48_000, 44_100)) {
            val detector = detector(sampleRate)
            for (f in testFrequencies) {
                val estimate = detector.detect(SyntheticSignals.sine(f, sampleRate, 4096))
                assertNotNull("No pitch for $f Hz @ $sampleRate", estimate)
                val error = SyntheticSignals.cents(estimate!!.frequencyHz, f)
                assertTrue("$f Hz @ $sampleRate off by $error cents", abs(error) <= 0.5)
                assertTrue("Clarity too low for a pure sine: ${estimate.clarity}", estimate.clarity > 0.95)
            }
        }
    }

    @Test
    fun `small detuning offsets are resolved precisely`() {
        val detector = detector(48_000)
        for (base in listOf(82.41, 110.0, 329.63)) {
            for (offset in listOf(-20.0, -5.0, -1.0, 1.0, 5.0, 20.0)) {
                val f = SyntheticSignals.withCents(base, offset)
                val estimate = detector.detect(SyntheticSignals.sine(f, 48_000, 4096))!!
                val measuredOffset = SyntheticSignals.cents(estimate.frequencyHz, base)
                assertEquals("offset $offset at $base Hz", offset, measuredOffset, 0.5)
            }
        }
    }

    @Test
    fun `string with weak fundamental is detected without octave errors`() {
        val detector = detector(48_000)
        for (f in testFrequencies.filter { it < 400 }) {
            val signal = SyntheticSignals.string(f, 48_000, 4096, fundamentalGainDb = -20.0)
            val estimate = detector.detect(signal)
            assertNotNull("No pitch for string at $f Hz", estimate)
            val error = SyntheticSignals.cents(estimate!!.frequencyHz, f)
            assertTrue("String at $f Hz off by $error cents (octave error?)", abs(error) <= 1.0)
        }
    }

    @Test
    fun `missing fundamental still yields the true pitch`() {
        val detector = detector(48_000)
        val f = 82.41
        val signal = SyntheticSignals.string(f, 48_000, 4096, fundamentalGainDb = -120.0)
        val estimate = detector.detect(signal)!!
        assertEquals(0.0, SyntheticSignals.cents(estimate.frequencyHz, f), 1.0)
    }

    @Test
    fun `noisy raw frame at 20 dB SNR stays within one and a half cents`() {
        val detector = detector(48_000)
        for (f in listOf(73.42, 82.41, 110.0, 146.83, 196.0, 246.94, 329.63)) {
            val clean = SyntheticSignals.string(f, 48_000, 4096, fundamentalGainDb = -10.0)
            val estimate = detector.detect(SyntheticSignals.withNoise(clean, snrDb = 20.0))
            assertNotNull(estimate)
            val error = SyntheticSignals.cents(estimate!!.frequencyHz, f)
            assertTrue("Noisy $f Hz off by $error cents", abs(error) <= 1.5)
        }
    }

    @Test
    fun `silence returns null`() {
        assertNull(detector(48_000).detect(FloatArray(4096)))
    }

    @Test
    fun `white noise has low clarity`() {
        val estimate = detector(48_000).detect(SyntheticSignals.noise(4096, amplitude = 0.3))
        assertTrue("Noise clarity should be low, was ${estimate?.clarity}", estimate == null || estimate.clarity < 0.6)
    }

    @Test
    fun `strummed chord has lower clarity than a single string`() {
        val single = SyntheticSignals.string(82.41, 48_000, 4096)
        val chord = FloatArray(4096)
        // Open E major: E2 B2 E3 G#3 B3 E4
        for (f in listOf(82.41, 123.47, 164.81, 207.65, 246.94, 329.63)) {
            val s = SyntheticSignals.string(f, 48_000, 4096, amplitude = 0.15)
            for (i in chord.indices) chord[i] += s[i]
        }
        val detector = detector(48_000)
        val singleClarity = detector.detect(single)!!.clarity
        val chordClarity = detector.detect(chord)?.clarity ?: 0.0
        assertTrue("chord=$chordClarity single=$singleClarity", chordClarity < singleClarity)
    }

    @Test
    fun `frame shorter than the window is rejected`() {
        val result = runCatching { detector(48_000).detect(FloatArray(100)) }
        assertTrue(result.isFailure)
    }
}
