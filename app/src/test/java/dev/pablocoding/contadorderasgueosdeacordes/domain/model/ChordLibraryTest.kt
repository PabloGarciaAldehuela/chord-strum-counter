package dev.pablocoding.contadorderasgueosdeacordes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `getChord returns null for unknown or empty chord names`() {
        assertNull(ChordLibrary.getChord("UnknownChord"))
        assertNull(ChordLibrary.getChord(""))
        assertNull(ChordLibrary.getChord("XYZ123"))
    }

    @Test
    fun `allChords contains unique chord names without duplicates`() {
        val names = ChordLibrary.allChords.map { it.name.lowercase() }
        val uniqueNames = names.toSet()
        assertEquals("All chords in ChordLibrary must have unique names", uniqueNames.size, names.size)
    }

    @Test
    fun `all chords have valid finger values and fret bounds`() {
        for (chord in ChordLibrary.allChords) {
            // Fingers: 0 = none, 1 = index, 2 = middle, 3 = ring, 4 = pinky
            for (finger in chord.fingers) {
                assertTrue("Finger $finger for ${chord.name} must be between 0 and 4", finger in 0..4)
            }

            // Frets: -1 = muted, 0 = open, 1..24 = fret number
            for (fret in chord.frets) {
                assertTrue("Fret $fret for ${chord.name} must be >= -1 and <= 24", fret in -1..24)
            }
        }
    }
}
