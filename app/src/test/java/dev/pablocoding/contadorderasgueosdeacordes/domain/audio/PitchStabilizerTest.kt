package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchStabilizerTest {

    private val room = 0.0005      // ~-66 dBFS
    private val string = 0.01      // ~-40 dBFS
    private val quietClick = 0.0002
    private val loudClick = 0.005

    /** Steady reading mode unless [live]: most tests cover the note logic shared by both modes. */
    private fun stabilizer(live: Boolean = false) = PitchStabilizer().also { it.followsLivePitch = live }

    /** Feeds hops every ~21 ms in [fromMs, toMs) and returns the last sample. */
    private fun PitchStabilizer.feed(
        fromMs: Long,
        toMs: Long,
        level: Double,
        estimate: PitchEstimate?,
        transient: Double = quietClick
    ): PitchSample {
        var last: PitchSample? = null
        var t = fromMs
        while (t < toMs) {
            last = process(estimate, level, t, transientLevel = transient)
            t += 21
        }
        return last!!
    }

    /** Quiet room for 1 s, then a pluck (level jump with a click) at 1000 ms. */
    private fun PitchStabilizer.pluckAt1s(frequency: Double): PitchSample {
        feed(0, 1000, room, null)
        return process(PitchEstimate(frequency, 0.95), string, 1000, transientLevel = loudClick)
    }

    @Test
    fun `quiet room never produces a reading`() {
        val s = stabilizer()
        val out = s.feed(0, 3000, room, PitchEstimate(110.0, 0.99))
        assertNull(out.frequencyHz)
        assertFalse(out.isSignalPresent)
    }

    @Test
    fun `a steady background tone is never displayed`() {
        // The original bug: a mains hum louder than the warm-up silence was shown as a note.
        val s = stabilizer()
        s.feed(0, 300, 0.0, null)                           // start-up silence from the phone
        val out = s.feed(300, 5000, 0.003, PitchEstimate(50.0, 0.99))
        assertNull("hum displayed as ${out.frequencyHz}", out.frequencyHz)
        assertFalse(out.isNoteActive)
    }

    @Test
    fun `a pluck starts a note, skips the attack, then reads unsettled before settling`() {
        val s = stabilizer()
        s.pluckAt1s(110.0)
        assertTrue(s.isNoteActive)
        assertNull("attack must be skipped", s.process(PitchEstimate(110.0, 0.95), string, 1042).frequencyHz)
        val early = s.process(PitchEstimate(110.0, 0.95), string, 1105)
        assertEquals(110.0, early.frequencyHz!!, 1e-6)
        assertFalse("first 250 ms are unsettled (attack glide)", early.isSettled)
        val later = s.feed(1126, 1400, string, PitchEstimate(110.0, 0.95))
        assertTrue(later.isSettled)
    }

    @Test
    fun `settling discards the sharp attack glide`() {
        val s = stabilizer()
        s.pluckAt1s(110.0)
        val sharp = SyntheticSignals.withCents(110.0, 8.0)
        s.feed(1084, 1250, string, PitchEstimate(sharp, 0.95))
        val settled = s.feed(1250, 1400, string, PitchEstimate(110.0, 0.95))
        assertTrue(settled.isSettled)
        assertEquals("settled value must not average in the glide", 110.0, settled.frequencyHz!!, 1e-6)
    }

    @Test
    fun `steady mode keeps the early-sustain value while the ringing string sinks`() {
        val s = stabilizer()
        s.pluckAt1s(82.41)
        // Starts 2 cents sharp, sinks 1 cent every 100 ms (a real low E sinks 5-10 cents in a second).
        var out: PitchSample? = null
        for (t in 1021L until 2400L step 21) {
            out = s.process(PitchEstimate(SyntheticSignals.withCents(82.41, 2.0 - (t - 1000) / 100.0), 0.95), string, t)
        }
        val anchoredCents = PitchMath.cents(out!!.frequencyHz!!, 82.41)
        assertTrue("anchored at the early sustain, got $anchoredCents", anchoredCents in -5.0..-2.0)
    }

    @Test
    fun `live mode follows the note from the pluck, attack included`() {
        val s = stabilizer(live = true)
        s.pluckAt1s(82.41)
        val early = s.process(PitchEstimate(SyntheticSignals.withCents(82.41, 2.0), 0.95), string, 1105)
        assertTrue("in-tune decisions start right after the attack", early.isSettled)
        assertEquals(2.0, PitchMath.cents(early.frequencyHz!!, 82.41), 0.01)
        var out: PitchSample? = null
        for (t in 1126L until 2400L step 21) {
            out = s.process(PitchEstimate(SyntheticSignals.withCents(82.41, 2.0 - (t - 1000) / 100.0), 0.95), string, t)
        }
        // Follows the sinking pitch (cluster mean over the last few hops, ~1 cent behind).
        assertEquals(-11.8, PitchMath.cents(out!!.frequencyHz!!, 82.41), 1.5)
    }

    @Test
    fun `low confidence estimates are rejected`() {
        val s = stabilizer()
        s.pluckAt1s(110.0)
        assertNull(s.feed(1084, 1500, string, PitchEstimate(110.0, 0.5)).frequencyHz)
    }

    @Test
    fun `a single spike is ignored and a sustained new note takes over after confirmation`() {
        val s = stabilizer()
        s.pluckAt1s(110.0)
        s.feed(1084, 1500, string, PitchEstimate(110.0, 0.95))
        assertEquals(110.0, s.process(PitchEstimate(220.0, 0.95), string, 1505).frequencyHz!!, 1e-6)
        s.process(PitchEstimate(146.83, 0.95), string, 1526)
        s.process(PitchEstimate(146.83, 0.95), string, 1547)
        assertEquals(146.83, s.process(PitchEstimate(146.83, 0.95), string, 1568).frequencyHz!!, 1e-6)
    }

    @Test
    fun `decay holds the last value then the note releases and clears`() {
        val s = stabilizer()
        s.pluckAt1s(82.41)
        s.feed(1084, 1600, string, PitchEstimate(82.41, 0.95))
        val held = s.process(null, room, 1900)
        assertTrue(held.isHolding)
        assertEquals(82.41, held.frequencyHz!!, 1e-6)
        val cleared = s.feed(1921, 2800, room, null)
        assertNull(cleared.frequencyHz)
        assertFalse(cleared.isNoteActive)
    }

    @Test
    fun `blocked frames hold without analysing`() {
        val s = stabilizer()
        s.pluckAt1s(196.0)
        s.feed(1084, 1500, string, PitchEstimate(196.0, 0.95))
        val blocked = s.process(PitchEstimate(1000.0, 0.99), 0.5, 1505, blocked = true)
        assertTrue(blocked.isHolding)
        assertEquals(196.0, blocked.frequencyHz!!, 1e-6)
    }

    @Test
    fun `a swell without a pluck click does not restart the note`() {
        val s = stabilizer()
        s.pluckAt1s(329.6)
        s.feed(1021, 1600, string, PitchEstimate(329.6, 0.95))
        val onsets = s.onsetCount
        // Strings beating against each other: +4 dB swell, no click.
        s.feed(1600, 1700, string * 1.7, PitchEstimate(329.6, 0.95), transient = quietClick)
        assertEquals(onsets, s.onsetCount)
        // A real re-pluck has a click.
        s.process(PitchEstimate(329.6, 0.95), string * 3.5, 1721, transientLevel = loudClick)
        assertEquals(onsets + 1, s.onsetCount)
    }

    @Test
    fun `a very strong jump counts as a pluck even without a click`() {
        val s = stabilizer()
        s.feed(0, 1000, room, null)
        s.process(PitchEstimate(110.0, 0.95), string, 1000, transientLevel = quietClick)
        assertTrue(s.isNoteActive)
    }

    @Test
    fun `click and bloom of one pluck count as a single onset`() {
        val s = stabilizer()
        s.pluckAt1s(110.0)
        s.process(PitchEstimate(110.0, 0.95), string * 3, 1150, transientLevel = loudClick * 3)
        assertEquals(1, s.onsetCount)
    }

    @Test
    fun `the floor follows a louder room within a couple of seconds`() {
        val s = stabilizer()
        s.feed(0, 1000, room, null)
        s.feed(1000, 5000, 0.004, null)        // a fan switches on: a "note" without pitch, released
        assertNotNull(s.noiseFloor)
        assertEquals(0.004, s.noiseFloor!!, 1e-9)
        assertFalse(s.isNoteActive)
        assertFalse(s.process(PitchEstimate(110.0, 0.95), 0.006, 5021, transientLevel = loudClick).isNoteActive)
    }

    @Test
    fun `finger or thumb pluck without click activates note`() {
        val s = stabilizer(live = true)
        s.feed(0, 1000, room, null)
        // Gentle pluck: level is 0.002 (above floor * gateRatio = 0.0005 * 2.8 = 0.0014)
        // No click at all: transientLevel = quietClick
        s.process(PitchEstimate(110.0, 0.90), 0.002, 1000, transientLevel = quietClick)
        assertTrue("Clear pitch above noise floor must activate note even without click", s.isNoteActive)
    }
}
