package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InTuneTrackerTest {

    @Test
    fun `enters only after staying within tolerance for the hold time`() {
        val tracker = InTuneTracker(holdMs = 400)
        assertFalse(tracker.update(1.0, nowMs = 0, toleranceCents = 3.0))
        assertFalse(tracker.update(-2.0, nowMs = 200, toleranceCents = 3.0))
        assertFalse(tracker.update(2.5, nowMs = 399, toleranceCents = 3.0))
        assertTrue(tracker.update(0.5, nowMs = 400, toleranceCents = 3.0))
    }

    @Test
    fun `leaving tolerance restarts the hold timer`() {
        val tracker = InTuneTracker(holdMs = 400)
        tracker.update(1.0, nowMs = 0, toleranceCents = 3.0)
        tracker.update(8.0, nowMs = 300, toleranceCents = 3.0)
        assertFalse(tracker.update(1.0, nowMs = 500, toleranceCents = 3.0))
        assertTrue(tracker.update(1.0, nowMs = 900, toleranceCents = 3.0))
    }

    @Test
    fun `exit needs to drift beyond the hysteresis margin`() {
        val tracker = InTuneTracker(holdMs = 0, exitMarginCents = 2.0)
        assertTrue(tracker.update(0.0, nowMs = 0, toleranceCents = 3.0))
        assertTrue("4¢ is inside the 3+2 exit band", tracker.update(4.0, nowMs = 20, toleranceCents = 3.0))
        assertTrue(tracker.update(-5.0, nowMs = 40, toleranceCents = 3.0))
        assertFalse(tracker.update(5.5, nowMs = 60, toleranceCents = 3.0))
    }

    @Test
    fun `reset clears the state`() {
        val tracker = InTuneTracker(holdMs = 0)
        tracker.update(0.0, nowMs = 0, toleranceCents = 3.0)
        tracker.reset()
        assertFalse(tracker.isInTune)
    }
}
