package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the smaller building blocks of the robust pipeline. */
class TunerComponentsTest {

    private val standard = TuningLibrary.standard.strings.map { PitchMath.midiToFrequency(it) }

    // ── OctaveResolver ──

    @Test
    fun `octave-up error on the A string is folded back when the signal supports it`() {
        // Real capture: weak fundamental + weak odd harmonics → MPM read 224 Hz for the A string.
        val resolved = OctaveResolver.resolve(224.0, standard) { 0.9 }
        assertEquals(112.0, resolved, 1e-9)
    }

    @Test
    fun `octave-down error on the B string is folded back`() {
        val resolved = OctaveResolver.resolve(123.1, standard) { 0.9 }
        assertEquals(246.2, resolved, 1e-9)
    }

    @Test
    fun `octave error on detuned string is corrected`() {
        // A string tuned 40 cents flat (80.5 Hz for Low E = 82.41 Hz).
        // Octave doubling error reads 161.0 Hz.
        val resolved = OctaveResolver.resolve(161.0, standard) { 0.9 }
        assertEquals(80.5, resolved, 0.1)
    }

    @Test
    fun `genuine detuning is not replaced by worse octave alternative`() {
        // Clean D3 (146.83 Hz) played 80 cents sharp is 153.8 Hz.
        // Even though 76.9 Hz is periodic and within 200 cents of Low E (82.41 Hz),
        // 120 cents distance is worse than the original 80 cents, so it must not fold.
        val resolved = OctaveResolver.resolve(153.8, standard) { 0.9 }
        assertEquals(153.8, resolved, 0.1)
    }

    @Test
    fun `an alternative without periodicity support is not taken`() {
        val resolved = OctaveResolver.resolve(224.0, standard) { f -> if (f == 224.0) 0.95 else 0.2 }
        assertEquals(224.0, resolved, 1e-9)
    }

    @Test
    fun `estimates near a string and chromatic mode are left alone`() {
        assertEquals(108.0, OctaveResolver.resolve(108.0, standard) { 0.9 }, 1e-9)
        assertEquals(224.0, OctaveResolver.resolve(224.0, emptyList()) { 0.9 }, 1e-9)
    }

    // ── NoiseFloorEstimator ──

    @Test
    fun `floor ignores the start-up silence and transients`() {
        val floor = NoiseFloorEstimator()
        for (t in 0 until 300L step 21) floor.update(0.0, t)               // mic warm-up silence
        assertNull(floor.floor)
        for (t in 300 until 2000L step 21) floor.update(if (t % 400 < 42) 0.05 else 0.001, t) // room + speech bursts
        assertEquals(0.001, floor.floor!!, 1e-12)
    }

    @Test
    fun `floor is frozen while a note rings`() {
        val floor = NoiseFloorEstimator()
        for (t in 0 until 1500L step 21) floor.update(0.001, t)
        for (t in 1500 until 5000L step 21) floor.update(0.02, t, frozen = true)
        assertEquals(0.001, floor.floor!!, 1e-12)
    }

    // ── BackgroundToneLearner ──

    @Test
    fun `a steady hum is learned while idle and its harmonics are notched, sparing string harmonics`() {
        val learner = BackgroundToneLearner()
        var learned: Double? = null
        for (t in 0..1100L step 21) learned = learner.observe(PitchEstimate(58.3, 0.9), t, idle = true, targetsHz = standard) ?: learned
        assertEquals(58.3, learned!!, 0.1)
        val notches = learner.notchFrequencies(58.3, standard, 1500.0)
        assertTrue(notches.isNotEmpty())
        notches.forEach { notch ->
            standard.forEach { target ->
                (1..8).forEach { h -> assertTrue("notch at $notch hits string harmonic", kotlin.math.abs(PitchMath.cents(notch, target * h)) >= 30) }
            }
        }
    }

    @Test
    fun `nothing is learned while playing or from a ringing string`() {
        val learner = BackgroundToneLearner()
        for (t in 0..3000L step 21) {
            assertNull(learner.observe(PitchEstimate(58.3, 0.9), t, idle = false, targetsHz = standard))
            assertNull(learner.observe(PitchEstimate(110.0, 0.99), t, idle = true, targetsHz = standard))
        }
        assertTrue(learner.tones.isEmpty())
    }

    // ── TunerAnalysisContext ──

    @Test
    fun `search range and high-pass follow the tuning`() {
        val ctx = TunerAnalysisContext(standard)
        assertEquals(82.41 * Math.pow(2.0, -7 / 12.0), ctx.searchRangeHz.start, 0.01)
        assertEquals(329.63 * Math.pow(2.0, 7 / 12.0), ctx.searchRangeHz.endInclusive, 0.01)
        assertEquals(82.41 * 0.7, ctx.highPassHz, 0.01)
        assertTrue(TunerAnalysisContext.CHROMATIC.isChromatic)
        assertEquals(40.0, TunerAnalysisContext.CHROMATIC.highPassHz, 0.0)
    }

    // ── HarmonicSpectrumDetector false-alarm rate ──

    @Test
    fun `room noise almost never produces a string candidate`() {
        val detector = HarmonicSpectrumDetector(48_000)
        var falseAlarms = 0
        repeat(200) { seed ->
            val noise = SyntheticSignals.noise(detector.windowSize, 0.01, seed = seed + 100)
            if (detector.analyze(noise, standard).isNotEmpty()) falseAlarms++
        }
        assertTrue("$falseAlarms/200 noise windows produced candidates", falseAlarms <= 2)
    }
}
