package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StringMatcherTest {

    private val standard = TuningLibrary.standard.strings // E2 A2 D3 G3 B3 E4 = 40 45 50 55 59 64
    private val dadgad = TuningLibrary.getTuning("dadgad").strings // 38 45 50 55 57 62

    @Test
    fun `picks the nearest string on the first reading`() {
        val matcher = StringMatcher()
        assertEquals(0, matcher.match(40.1, standard))
        val fresh = StringMatcher()
        assertEquals(4, fresh.match(58.7, standard))
    }

    @Test
    fun `a clearly different string switches immediately`() {
        val matcher = StringMatcher()
        assertEquals(0, matcher.match(40.0, standard))
        assertEquals(1, matcher.match(45.05, standard))
    }

    @Test
    fun `a string between two targets does not flap`() {
        val matcher = StringMatcher()
        // G string 120 cents sharp in DADGAD sits between G3 (55) and A3 (57).
        assertEquals(3, matcher.match(55.9, dadgad))
        // Slightly closer to A3 but within the 30-cent switch margin: stay on G3.
        assertEquals(3, matcher.match(56.1, dadgad))
        assertEquals(3, matcher.match(55.95, dadgad))
        assertEquals(3, matcher.match(56.1, dadgad))
    }

    @Test
    fun `moderate change needs several confirming frames`() {
        val matcher = StringMatcher(confirmFrames = 3)
        assertEquals(3, matcher.match(55.0, dadgad))
        // 1.2 semitones from G3, 0.8 from A3 → 40 cents gain, below the immediate-switch distance.
        assertEquals(3, matcher.match(56.2, dadgad))
        assertEquals(3, matcher.match(56.2, dadgad))
        assertEquals(4, matcher.match(56.2, dadgad))
    }

    @Test
    fun `reset forgets the current string`() {
        val matcher = StringMatcher()
        matcher.match(40.0, standard)
        matcher.reset()
        assertNull(matcher.currentIndex)
    }

    @Test
    fun `empty tunings are rejected`() {
        assertTrue(runCatching { StringMatcher().match(40.0, emptyList()) }.isFailure)
    }
}
