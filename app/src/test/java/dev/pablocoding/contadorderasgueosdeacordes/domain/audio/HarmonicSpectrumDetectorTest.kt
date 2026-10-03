package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HarmonicSpectrumDetectorTest {

    private val sampleRate = 48_000
    private val standard = TuningLibrary.standard.strings.map { PitchMath.midiToFrequency(it) }
    private val detector = HarmonicSpectrumDetector(sampleRate)
    private val n = detector.windowSize

    private fun best(frame: FloatArray) = detector.analyze(frame, standard).maxByOrNull { it.score }

    @Test
    fun `window is about 340 ms at 48 kHz`() {
        assertEquals(16384, HarmonicSpectrumDetector.windowSizeFor(48_000))
    }

    @Test
    fun `every open string is identified and measured within half a cent`() {
        for ((i, f) in standard.withIndex()) {
            for (offset in listOf(-30.0, -5.0, 0.0, 7.0, 40.0)) {
                val actual = SyntheticSignals.withCents(f, offset)
                val c = best(SyntheticSignals.string(actual, sampleRate, n, fundamentalGainDb = -15.0))
                assertNotNull(c)
                assertEquals("string index for $f", i, c!!.index)
                val error = SyntheticSignals.cents(c.frequencyHz, actual)
                assertTrue("$f Hz @ $offset¢ off by $error", abs(error) <= 0.5)
            }
        }
    }

    @Test
    fun `weak string below broadband noise is still found accurately`() {
        // Wideband SNR of -6 dB: MPM clarity collapses here, the harmonics still stand out in the FFT.
        for (f in standard) {
            val clean = SyntheticSignals.string(f, sampleRate, n, fundamentalGainDb = -15.0)
            val noisy = SyntheticSignals.withNoise(clean, snrDb = -6.0)
            val c = best(noisy)
            assertNotNull("no detection for $f", c)
            assertEquals(f, standard[c!!.index], 1e-9)
            val error = SyntheticSignals.cents(c.frequencyHz, f)
            assertTrue("$f Hz off by $error cents at -6 dB SNR", abs(error) <= 1.5)
        }
    }

    @Test
    fun `newly plucked string outscores a quieter one still ringing`() {
        val ringing = SyntheticSignals.string(82.41, sampleRate, n, amplitude = 0.1)
        val plucked = SyntheticSignals.string(110.0, sampleRate, n, amplitude = 0.3)
        val mix = FloatArray(n) { ringing[it] + plucked[it] }
        val candidates = detector.analyze(mix, standard)
        val a = candidates.first { it.index == 1 }
        assertEquals(0.0, SyntheticSignals.cents(a.frequencyHz, 110.0), 1.0)
        assertTrue(candidates.maxBy { it.score }.index == 1)
    }

    @Test
    fun `high E is not mistaken for the low E's upper harmonics`() {
        val c = best(SyntheticSignals.string(329.63, sampleRate, n, harmonics = 4))
        assertEquals(5, c!!.index)
    }

    @Test
    fun `noise alone yields no candidates`() {
        val candidates = detector.analyze(SyntheticSignals.noise(n, amplitude = 0.01), standard)
        assertTrue("noise produced $candidates", candidates.isEmpty())
    }

    @Test
    fun `chromatic mode (no targets) yields nothing`() {
        assertTrue(detector.analyze(SyntheticSignals.sine(440.0, sampleRate, n), emptyList()).isEmpty())
    }
}
