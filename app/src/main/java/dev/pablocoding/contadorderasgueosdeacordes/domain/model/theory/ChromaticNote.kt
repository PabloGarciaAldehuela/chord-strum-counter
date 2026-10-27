package dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory

enum class NoteNotationMode(val label: String) {
    LETTERS("Letters"),
    SOLFEGE("Solfège"),
    DUAL("Both")
}

data class GuitarFretPosition(
    val stringNumber: Int, // 1 to 6 (1 = High E, 6 = Low E)
    val stringName: String, // e.g. "6th (Low E)"
    val frets: List<Int>   // All frets on this string (0..12) where the pitch class sounds
)

data class ChromaticNote(
    val pitchClass: Int, // 0 = C, 1 = C#/Db, ..., 11 = B
    val nameLetters: String,
    val nameSolfege: String,
    val isNatural: Boolean,
    val isGluedWithNext: Boolean,
    val baseMidi: Int = 48 + pitchClass // C3 = 48
) {
    fun displayName(mode: NoteNotationMode): String = when (mode) {
        NoteNotationMode.LETTERS -> nameLetters
        NoteNotationMode.SOLFEGE -> nameSolfege
        NoteNotationMode.DUAL -> if (isNatural) {
            "$nameLetters ($nameSolfege)"
        } else {
            "$nameLetters\n($nameSolfege)"
        }
    }

    /** Single-line dual format e.g. "C (Do)" or "C# / Db (Do# / Reb)" */
    fun compactDualName(): String = if (isNatural) {
        "$nameLetters ($nameSolfege)"
    } else {
        "$nameLetters ($nameSolfege)"
    }
}

object ChromaticWheelData {

    val NOTES: List<ChromaticNote> = listOf(
        ChromaticNote(0, "C", "Do", isNatural = true, isGluedWithNext = false),
        ChromaticNote(1, "C♯ / D♭", "Do♯ / Re♭", isNatural = false, isGluedWithNext = false),
        ChromaticNote(2, "D", "Re", isNatural = true, isGluedWithNext = false),
        ChromaticNote(3, "D♯ / E♭", "Re♯ / Mi♭", isNatural = false, isGluedWithNext = false),
        ChromaticNote(4, "E", "Mi", isNatural = true, isGluedWithNext = true),
        ChromaticNote(5, "F", "Fa", isNatural = true, isGluedWithNext = false),
        ChromaticNote(6, "F♯ / G♭", "Fa♯ / Sol♭", isNatural = false, isGluedWithNext = false),
        ChromaticNote(7, "G", "Sol", isNatural = true, isGluedWithNext = false),
        ChromaticNote(8, "G♯ / A♭", "Sol♯ / La♭", isNatural = false, isGluedWithNext = false),
        ChromaticNote(9, "A", "La", isNatural = true, isGluedWithNext = false),
        ChromaticNote(10, "A♯ / B♭", "La♯ / Si♭", isNatural = false, isGluedWithNext = false),
        ChromaticNote(11, "B", "Si", isNatural = true, isGluedWithNext = true)
    )

    /** Standard guitar strings in physical order: 6th (Low E) to 1st (High E) */
    val GUITAR_STRINGS: List<Pair<Int, String>> = listOf(
        6 to "6th (Low E)",
        5 to "5th (A)",
        4 to "4th (D)",
        3 to "3rd (G)",
        2 to "2nd (B)",
        1 to "1st (High E)"
    )

    // Open string pitch classes: 6th=E(4), 5th=A(9), 4th=D(2), 3rd=G(7), 2nd=B(11), 1st=E(4)
    private val OPEN_PITCH_CLASSES = listOf(4, 9, 2, 7, 11, 4)

    /**
     * Calculates the fret positions (0..12) where a given pitch class occurs across all 6 strings.
     */
    fun fretPositionsFor(pitchClass: Int): List<GuitarFretPosition> {
        val result = mutableListOf<GuitarFretPosition>()
        for (i in 0 until 6) {
            val (stringNum, label) = GUITAR_STRINGS[i]
            val openPitch = OPEN_PITCH_CLASSES[i]
            val primaryFret = Math.floorMod(pitchClass - openPitch, 12)
            val frets = if (primaryFret == 0) listOf(0, 12) else listOf(primaryFret)
            result.add(GuitarFretPosition(stringNum, label, frets))
        }
        return result
    }

    /**
     * Pitch class of a string played at a specific fret (0..12).
     */
    fun pitchClassAt(stringIndex: Int, fret: Int): Int {
        val openPitch = OPEN_PITCH_CLASSES[stringIndex.coerceIn(0, 5)]
        return Math.floorMod(openPitch + fret, 12)
    }

    /**
     * Semitone distance moving clockwise from root to target.
     */
    fun semitonesBetween(rootPitchClass: Int, targetPitchClass: Int): Int =
        Math.floorMod(targetPitchClass - rootPitchClass, 12)

    /**
     * Educational interval description in English.
     */
    fun intervalName(semitones: Int): Pair<String, String> = when (Math.floorMod(semitones, 12)) {
        0 -> "Unison" to "Same note (0 frets)"
        1 -> "Minor 2nd" to "1 semitone (1 fret / half step)"
        2 -> "Major 2nd" to "1 whole step (2 frets)"
        3 -> "Minor 3rd" to "3 semitones (3 frets)"
        4 -> "Major 3rd" to "2 whole steps (4 frets)"
        5 -> "Perfect 4th" to "5 semitones (5 frets)"
        6 -> "Tritone (Aug 4th / Dim 5th)" to "6 semitones (6 frets)"
        7 -> "Perfect 5th" to "7 semitones (7 frets)"
        8 -> "Minor 6th" to "8 semitones (8 frets)"
        9 -> "Major 6th" to "9 semitones (9 frets)"
        10 -> "Minor 7th" to "10 semitones (10 frets)"
        11 -> "Major 7th" to "11 semitones (11 frets)"
        else -> "" to ""
    }
}
