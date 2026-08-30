package dev.pablocoding.contadorderasgueosdeacordes.domain.model

object ChordLibrary {

    val allChords: List<Chord> = listOf(
        // Major Chords
        Chord(
            name = "A",
            fullName = "A Major",
            frets = listOf(-1, 0, 2, 2, 2, 0),
            fingers = listOf(0, 0, 1, 2, 3, 0)
        ),
        Chord(
            name = "C",
            fullName = "C Major",
            frets = listOf(-1, 3, 2, 0, 1, 0),
            fingers = listOf(0, 3, 2, 0, 1, 0)
        ),
        Chord(
            name = "D",
            fullName = "D Major",
            frets = listOf(-1, -1, 0, 2, 3, 2),
            fingers = listOf(0, 0, 0, 1, 3, 2)
        ),
        Chord(
            name = "E",
            fullName = "E Major",
            frets = listOf(0, 2, 2, 1, 0, 0),
            fingers = listOf(0, 2, 3, 1, 0, 0)
        ),
        Chord(
            name = "G",
            fullName = "G Major",
            frets = listOf(3, 2, 0, 0, 0, 3),
            fingers = listOf(2, 1, 0, 0, 0, 3)
        ),
        Chord(
            name = "F",
            fullName = "F Major (Easy/Open)",
            frets = listOf(-1, -1, 3, 2, 1, 1),
            fingers = listOf(0, 0, 3, 2, 1, 1)
        ),

        // Minor Chords
        Chord(
            name = "Am",
            fullName = "A Minor",
            frets = listOf(-1, 0, 2, 2, 1, 0),
            fingers = listOf(0, 0, 2, 3, 1, 0)
        ),
        Chord(
            name = "Dm",
            fullName = "D Minor",
            frets = listOf(-1, -1, 0, 2, 3, 1),
            fingers = listOf(0, 0, 0, 2, 3, 1)
        ),
        Chord(
            name = "Em",
            fullName = "E Minor",
            frets = listOf(0, 2, 2, 0, 0, 0),
            fingers = listOf(0, 2, 3, 0, 0, 0)
        ),
        Chord(
            name = "Bm",
            fullName = "B Minor (Easy/Open)",
            frets = listOf(-1, 2, 4, 4, 3, 2),
            fingers = listOf(0, 1, 3, 4, 2, 1)
        ),

        // 7th Chords
        Chord(
            name = "A7",
            fullName = "A Dominant 7th",
            frets = listOf(-1, 0, 2, 0, 2, 0),
            fingers = listOf(0, 0, 2, 0, 3, 0)
        ),
        Chord(
            name = "C7",
            fullName = "C Dominant 7th",
            frets = listOf(-1, 3, 2, 3, 1, 0),
            fingers = listOf(0, 3, 2, 4, 1, 0)
        ),
        Chord(
            name = "D7",
            fullName = "D Dominant 7th",
            frets = listOf(-1, -1, 0, 2, 1, 2),
            fingers = listOf(0, 0, 0, 2, 1, 3)
        ),
        Chord(
            name = "E7",
            fullName = "E Dominant 7th",
            frets = listOf(0, 2, 0, 1, 0, 0),
            fingers = listOf(0, 2, 0, 1, 0, 0)
        ),
        Chord(
            name = "G7",
            fullName = "G Dominant 7th",
            frets = listOf(3, 2, 0, 0, 0, 1),
            fingers = listOf(3, 2, 0, 0, 0, 1)
        ),
        Chord(
            name = "B7",
            fullName = "B Dominant 7th",
            frets = listOf(-1, 2, 1, 2, 0, 2),
            fingers = listOf(0, 2, 1, 3, 0, 4)
        )
    )

    private val chordMap: Map<String, Chord> by lazy {
        allChords.associateBy { it.name.uppercase() }
    }

    fun getChord(name: String): Chord? = chordMap[name.trim().uppercase()]
}
