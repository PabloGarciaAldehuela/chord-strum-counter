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

    @Test
    fun `stuck 3 and 4 chords exist and have strings 1 and 2 anchored on fret 3 with fingers 3 and 4`() {
        val stuckChordNames = listOf(
            "Big G",
            "G5",
            "Cadd9",
            "Dsus4",
            "A7sus4",
            "A7sus4 (var)",
            "Em7",
            "Dadd11/F#",
            "F6/9"
        )
        for (name in stuckChordNames) {
            val chord = ChordLibrary.getChord(name)
            assertNotNull("Chord $name must exist in ChordLibrary", chord)
            assertEquals("String 2 (B) must be fret 3 for $name", 3, chord!!.frets[4])
            assertEquals("String 1 (high e) must be fret 3 for $name", 3, chord.frets[5])
            assertEquals("String 2 finger must be 3 for $name", 3, chord.fingers[4])
            assertEquals("String 1 finger must be 4 for $name", 4, chord.fingers[5])
        }
    }

    @Test
    fun `getChord resolves common aliases and variations`() {
        assertEquals("Cadd9", ChordLibrary.getChord("C add9")?.name)
        assertEquals("Dsus4", ChordLibrary.getChord("D sus4")?.name)
        assertEquals("A7sus4", ChordLibrary.getChord("A7 sus4")?.name)
        assertEquals("A7sus4", ChordLibrary.getChord("A7 SUS 4")?.name)
        assertEquals("A7sus4 (var)", ChordLibrary.getChord("A7sus4 (var)")?.name)
        assertEquals("A7sus4 (var)", ChordLibrary.getChord("A7SUS4(VAR)")?.name)
        assertEquals("A7sus4 (var)", ChordLibrary.getChord("A7sus4 (Open G)")?.name)
        assertEquals("Em7", ChordLibrary.getChord("E min7")?.name)
        assertEquals("Em7", ChordLibrary.getChord("Emin7")?.name)
        assertEquals("Dadd11/F#", ChordLibrary.getChord("D add11/F#")?.name)
        assertEquals("F6/9", ChordLibrary.getChord("F 6/9")?.name)
        assertEquals("G5", ChordLibrary.getChord("Rock G")?.name)
        assertEquals("Big G", ChordLibrary.getChord("Four Finger G")?.name)
    }

    @Test
    fun `stuck 3 and 4 chord frets match course diagrams exactly`() {
        val bigG = ChordLibrary.getChord("Big G")!!
        assertEquals(listOf(3, 2, 0, 0, 3, 3), bigG.frets)
        assertEquals(listOf(2, 1, 0, 0, 3, 4), bigG.fingers)

        val g5 = ChordLibrary.getChord("G5")!!
        assertEquals(listOf(3, -1, 0, 0, 3, 3), g5.frets)
        assertEquals(listOf(2, 0, 0, 0, 3, 4), g5.fingers)

        val cadd9 = ChordLibrary.getChord("Cadd9")!!
        assertEquals(listOf(-1, 3, 2, 0, 3, 3), cadd9.frets)
        assertEquals(listOf(0, 2, 1, 0, 3, 4), cadd9.fingers)

        val dsus4 = ChordLibrary.getChord("Dsus4")!!
        assertEquals(listOf(-1, -1, 0, 2, 3, 3), dsus4.frets)
        assertEquals(listOf(0, 0, 0, 1, 3, 4), dsus4.fingers)

        val a7sus4 = ChordLibrary.getChord("A7sus4")!!
        assertEquals(listOf(-1, 0, 2, 2, 3, 3), a7sus4.frets)
        assertEquals(listOf(0, 0, 1, 2, 3, 4), a7sus4.fingers)

        val a7sus4Var = ChordLibrary.getChord("A7sus4 (var)")!!
        assertEquals(listOf(-1, 0, 2, 0, 3, 3), a7sus4Var.frets)
        assertEquals(listOf(0, 0, 1, 0, 3, 4), a7sus4Var.fingers)

        val em7 = ChordLibrary.getChord("Em7")!!
        assertEquals(listOf(0, 2, 2, 0, 3, 3), em7.frets)
        assertEquals(listOf(0, 1, 2, 0, 3, 4), em7.fingers)

        val dadd11 = ChordLibrary.getChord("Dadd11/F#")!!
        assertEquals(listOf(2, -1, 0, 2, 3, 3), dadd11.frets)
        assertEquals(listOf(1, 0, 0, 2, 3, 4), dadd11.fingers)

        val f69 = ChordLibrary.getChord("F6/9")!!
        assertEquals(listOf(-1, -1, 3, 2, 3, 3), f69.frets)
        assertEquals(listOf(0, 0, 2, 1, 3, 4), f69.fingers)
    }
}
