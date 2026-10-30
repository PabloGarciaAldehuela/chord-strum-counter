package dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromaticNoteTest {

    @Test
    fun chromaticWheelContains12Notes() {
        val notes = ChromaticWheelData.NOTES
        assertEquals(12, notes.size)
        assertEquals(0, notes.first().pitchClass)
        assertEquals("C", notes.first().nameLetters)
        assertEquals(11, notes.last().pitchClass)
        assertEquals("B", notes.last().nameLetters)
    }

    @Test
    fun naturalNotesAndGluedPairsMatchMusicTheory() {
        val notes = ChromaticWheelData.NOTES

        // 7 natural notes: C, D, E, F, G, A, B
        val naturalNotes = notes.filter { it.isNatural }
        assertEquals(7, naturalNotes.size)
        assertEquals(listOf("C", "D", "E", "F", "G", "A", "B"), naturalNotes.map { it.nameLetters })

        // 5 accidental notes
        val accidentalNotes = notes.filterNot { it.isNatural }
        assertEquals(5, accidentalNotes.size)

        // Glued natural pairs: E (pitchClass 4) and B (pitchClass 11)
        val gluedNotes = notes.filter { it.isGluedWithNext }
        assertEquals(2, gluedNotes.size)
        assertEquals(listOf(4, 11), gluedNotes.map { it.pitchClass })
        assertEquals(listOf("E", "B"), gluedNotes.map { it.nameLetters })

        // Verify E -> F is adjacent (pitch 4 to 5)
        assertEquals(4, notes[4].pitchClass)
        assertEquals(5, notes[5].pitchClass)
        assertTrue(notes[4].isGluedWithNext)

        // Verify B -> C is adjacent (pitch 11 to 0)
        assertEquals(11, notes[11].pitchClass)
        assertEquals(0, notes[0].pitchClass)
        assertTrue(notes[11].isGluedWithNext)
    }

    @Test
    fun notationDisplayModesFormatCorrectly() {
        val cNote = ChromaticWheelData.NOTES[0]
        assertEquals("C", cNote.displayName(NoteNotationMode.LETTERS))
        assertEquals("Do", cNote.displayName(NoteNotationMode.SOLFEGE))
        assertEquals("C (Do)", cNote.displayName(NoteNotationMode.DUAL))

        val cSharpNote = ChromaticWheelData.NOTES[1]
        assertEquals("C♯ / D♭", cSharpNote.displayName(NoteNotationMode.LETTERS))
        assertEquals("Do♯ / Re♭", cSharpNote.displayName(NoteNotationMode.SOLFEGE))
    }

    @Test
    fun guitarFretPositionsForC() {
        // C is pitch class 0
        val positions = ChromaticWheelData.fretPositionsFor(0)
        assertEquals(6, positions.size)

        // 6th string Low E (open 4) -> (0 - 4) % 12 = 8th fret
        val string6 = positions.first { it.stringNumber == 6 }
        assertEquals(listOf(8), string6.frets)

        // 5th string A (open 9) -> (0 - 9) % 12 = 3rd fret
        val string5 = positions.first { it.stringNumber == 5 }
        assertEquals(listOf(3), string5.frets)

        // 4th string D (open 2) -> (0 - 2) % 12 = 10th fret
        val string4 = positions.first { it.stringNumber == 4 }
        assertEquals(listOf(10), string4.frets)

        // 3rd string G (open 7) -> (0 - 7) % 12 = 5th fret
        val string3 = positions.first { it.stringNumber == 3 }
        assertEquals(listOf(5), string3.frets)

        // 2nd string B (open 11) -> (0 - 11) % 12 = 1st fret
        val string2 = positions.first { it.stringNumber == 2 }
        assertEquals(listOf(1), string2.frets)

        // 1st string High E (open 4) -> 8th fret
        val string1 = positions.first { it.stringNumber == 1 }
        assertEquals(listOf(8), string1.frets)
    }

    @Test
    fun guitarFretPositionsForOpenEIncludes0And12() {
        // E is pitch class 4
        val positions = ChromaticWheelData.fretPositionsFor(4)
        val string6 = positions.first { it.stringNumber == 6 }
        assertEquals(listOf(0, 12), string6.frets)
    }

    @Test
    fun pitchClassAtStringAndFretMatchesPhysicalGuitar() {
        // 6th string (index 0, open E = 4)
        assertEquals(4, ChromaticWheelData.pitchClassAt(0, 0)) // E
        assertEquals(5, ChromaticWheelData.pitchClassAt(0, 1)) // F (pegado a E!)
        assertEquals(6, ChromaticWheelData.pitchClassAt(0, 2)) // F#
        assertEquals(7, ChromaticWheelData.pitchClassAt(0, 3)) // G
    }

    @Test
    fun semitonesBetweenCalculatesClockwiseDistance() {
        // C (0) to G (7) = 7 semitones (5th)
        assertEquals(7, ChromaticWheelData.semitonesBetween(0, 7))

        // G (7) to C (0) = 5 semitones (4th)
        assertEquals(5, ChromaticWheelData.semitonesBetween(7, 0))

        // E (4) to F (5) = 1 semitone (natural half-step)
        assertEquals(1, ChromaticWheelData.semitonesBetween(4, 5))

        // B (11) to C (0) = 1 semitone (natural half-step)
        assertEquals(1, ChromaticWheelData.semitonesBetween(11, 0))
    }

    @Test
    fun intervalNamesMatchStandardMusicTheory() {
        val (name0, _) = ChromaticWheelData.intervalName(0)
        assertEquals("Unison", name0)

        val (name1, _) = ChromaticWheelData.intervalName(1)
        assertEquals("Minor 2nd", name1)

        val (name4, _) = ChromaticWheelData.intervalName(4)
        assertEquals("Major 3rd", name4)

        val (name7, _) = ChromaticWheelData.intervalName(7)
        assertEquals("Perfect 5th", name7)
    }
}
