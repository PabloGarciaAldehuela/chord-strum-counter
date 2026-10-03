package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchMathTest {

    @Test
    fun `A4 maps to the reference pitch and E2 to 82_41 Hz`() {
        assertEquals(440.0, PitchMath.midiToFrequency(69), 1e-9)
        assertEquals(82.41, PitchMath.midiToFrequency(40), 0.005)
        assertEquals(329.63, PitchMath.midiToFrequency(64), 0.005)
    }

    @Test
    fun `reference pitch calibration shifts every target proportionally`() {
        assertEquals(432.0, PitchMath.midiToFrequency(69, referenceHz = 432.0), 1e-9)
        assertEquals(82.41 * 445.0 / 440.0, PitchMath.midiToFrequency(40, referenceHz = 445.0), 0.005)
    }

    @Test
    fun `frequency to midi is the inverse of midi to frequency`() {
        for (midi in 28..88) {
            val f = PitchMath.midiToFrequency(midi, 442.0)
            assertEquals(midi.toDouble(), PitchMath.frequencyToMidi(f, 442.0), 1e-9)
            assertEquals(midi, PitchMath.nearestMidi(f, 442.0))
        }
    }

    @Test
    fun `cents are negative when flat and positive when sharp`() {
        assertEquals(0.0, PitchMath.cents(110.0, 110.0), 1e-9)
        assertEquals(1200.0, PitchMath.cents(220.0, 110.0), 1e-9)
        assertTrue(PitchMath.cents(109.0, 110.0) < 0)
        assertTrue(PitchMath.cents(111.0, 110.0) > 0)
        assertEquals(100.0, PitchMath.cents(PitchMath.midiToFrequency(41), PitchMath.midiToFrequency(40)), 1e-9)
    }

    @Test
    fun `note names respect sharps, flats and solfege`() {
        assertEquals("E", PitchMath.noteName(40))
        assertEquals("C♯", PitchMath.noteName(49))
        assertEquals("D♭", PitchMath.noteName(49, preferFlats = true))
        assertEquals("Mi", PitchMath.noteName(40, naming = NoteNaming.SOLFEGE))
        assertEquals("Sol♯", PitchMath.noteName(56, naming = NoteNaming.SOLFEGE))
        assertEquals("La♭", PitchMath.noteName(56, preferFlats = true, naming = NoteNaming.SOLFEGE))
    }

    @Test
    fun `octaves follow scientific pitch notation`() {
        assertEquals(2, PitchMath.octave(40))
        assertEquals(4, PitchMath.octave(60))
        assertEquals(3, PitchMath.octave(59))
        assertEquals(-1, PitchMath.octave(0))
    }

    @Test
    fun `parses scientific pitch notation`() {
        assertEquals(40, PitchMath.parseNote("E2"))
        assertEquals(33, PitchMath.parseNote("A1"))
        assertEquals(37, PitchMath.parseNote("C#2"))
        assertEquals(39, PitchMath.parseNote("Eb2"))
        assertEquals(35, PitchMath.parseNote("B1"))
        assertEquals(69, PitchMath.parseNote("A4"))
    }

    @Test
    fun `rejects malformed notes`() {
        for (bad in listOf("", "H2", "E", "Ex")) {
            assertTrue("'$bad' should be rejected", runCatching { PitchMath.parseNote(bad) }.isFailure)
        }
    }

    @Test
    fun `solfege is the default for spanish and similar locales`() {
        assertEquals(NoteNaming.SOLFEGE, NoteNaming.defaultForLanguage("es"))
        assertEquals(NoteNaming.SOLFEGE, NoteNaming.defaultForLanguage("IT"))
        assertEquals(NoteNaming.LETTERS, NoteNaming.defaultForLanguage("en"))
        assertEquals(NoteNaming.LETTERS, NoteNaming.defaultForLanguage("de"))
    }
}
