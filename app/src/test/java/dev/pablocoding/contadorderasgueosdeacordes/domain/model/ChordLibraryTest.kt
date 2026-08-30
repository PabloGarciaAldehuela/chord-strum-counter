package dev.pablocoding.contadorderasgueosdeacordes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordLibraryTest {

    @Test
    fun `all chords have exactly 6 string fret definitions`() {
        val chords = ChordLibrary.allChords
        assertTrue(chords.isNotEmpty())

        for (chord in chords) {
            assertEquals("Chord ${chord.name} frets size must be 6", 6, chord.frets.size)
            assertEquals("Chord ${chord.name} fingers size must be 6", 6, chord.fingers.size)
            assertTrue("Chord ${chord.name} baseFret must be >= 1", chord.baseFret >= 1)
        }
    }

    @Test
    fun `getChord finds chord case-insensitively`() {
        val chordA = ChordLibrary.getChord("a")
        assertNotNull(chordA)
        assertEquals("A", chordA?.name)
        assertEquals("A Major", chordA?.fullName)

        val chordEm = ChordLibrary.getChord("EM")
        assertNotNull(chordEm)
        assertEquals("Em", chordEm?.name)
    }
}
