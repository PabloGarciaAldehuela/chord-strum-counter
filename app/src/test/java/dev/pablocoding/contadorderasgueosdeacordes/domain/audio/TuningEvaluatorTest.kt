package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningEvaluatorTest {

    private val standard = TuningLibrary.standard
    private val dropD = TuningLibrary.getTuning("drop_d")

    private fun sample(frequency: Double?, t: Long, holding: Boolean = false, signal: Boolean = frequency != null) =
        PitchSample(frequency, clarity = 0.95, level = 0.1, isHolding = holding, isSignalPresent = signal, timestampMs = t)

    @Test
    fun `auto mode matches the nearest string and reports cents`() {
        val evaluator = TuningEvaluator()
        val flatA = SyntheticSignals.withCents(110.0, -12.0)
        val reading = evaluator.evaluate(sample(flatA, 0), standard, 440.0, null, 3.0)
        assertEquals(1, reading.targetStringIndex)
        assertEquals(45, reading.targetMidi)
        assertEquals(-12.0, reading.cents!!, 1e-6)
        assertFalse(reading.isInTune)
    }

    @Test
    fun `a locked string is used even when another string is closer`() {
        val evaluator = TuningEvaluator()
        // Retuning E2 down to D2 for Drop D: the low string currently rings at E2.
        val reading = evaluator.evaluate(sample(82.41, 0), dropD, 440.0, lockedStringIndex = 0, toleranceCents = 3.0)
        assertEquals(0, reading.targetStringIndex)
        assertEquals(38, reading.targetMidi)
        assertEquals(200.0, reading.cents!!, 0.1)
    }

    @Test
    fun `chromatic mode targets the nearest semitone`() {
        val evaluator = TuningEvaluator()
        val reading = evaluator.evaluate(sample(SyntheticSignals.withCents(277.18, 8.0), 0), TuningLibrary.chromatic, 440.0, null, 3.0)
        assertNull(reading.targetStringIndex)
        assertEquals(61, reading.targetMidi)
        assertEquals(8.0, reading.cents!!, 0.05)
    }

    @Test
    fun `reference pitch moves the targets`() {
        val evaluator = TuningEvaluator()
        val reading = evaluator.evaluate(sample(110.0, 0), standard, 432.0, null, 3.0)
        assertEquals(1200.0 * Math.log(440.0 / 432.0) / Math.log(2.0), reading.cents!!, 0.01)
    }

    @Test
    fun `in tune is signalled once after the hold time`() {
        val evaluator = TuningEvaluator()
        val readings = (0..600L step 20).map { evaluator.evaluate(sample(146.83, it), standard, 440.0, null, 3.0) }
        assertEquals(1, readings.count { it.justBecameInTune })
        assertTrue(readings.last().isInTune)
        assertEquals(2, readings.last().targetStringIndex)
    }

    @Test
    fun `held samples keep the in-tune state and target`() {
        val evaluator = TuningEvaluator()
        (0..600L step 20).forEach { evaluator.evaluate(sample(196.0, it), standard, 440.0, null, 3.0) }
        val held = evaluator.evaluate(sample(196.0, 800, holding = true), standard, 440.0, null, 3.0)
        assertTrue(held.isInTune)
        assertTrue(held.isHolding)
        assertEquals(3, held.targetStringIndex)
    }

    @Test
    fun `losing the signal resets the in-tune state`() {
        val evaluator = TuningEvaluator()
        (0..600L step 20).forEach { evaluator.evaluate(sample(196.0, it), standard, 440.0, null, 3.0) }
        val silent = evaluator.evaluate(sample(null, 2000), standard, 440.0, null, 3.0)
        assertFalse(silent.isInTune)
        assertFalse(silent.hasPitch)
    }

    @Test
    fun `noise hint appears after three seconds of unusable signal`() {
        val evaluator = TuningEvaluator(noiseHintDelayMs = 3000)
        val early = evaluator.evaluate(sample(null, 0, signal = true), standard, 440.0, null, 3.0)
        val late = evaluator.evaluate(sample(null, 3000, signal = true), standard, 440.0, null, 3.0)
        val recovered = evaluator.evaluate(sample(110.0, 3020), standard, 440.0, null, 3.0)
        assertFalse(early.showNoiseHint)
        assertTrue(late.showNoiseHint)
        assertFalse(recovered.showNoiseHint)
    }

    @Test
    fun `silent reading keeps the locked target for display`() {
        val evaluator = TuningEvaluator()
        val reading = evaluator.evaluate(sample(null, 0), standard, 440.0, lockedStringIndex = 4, toleranceCents = 3.0)
        assertEquals(4, reading.targetStringIndex)
        assertEquals(59, reading.targetMidi)
    }
}
