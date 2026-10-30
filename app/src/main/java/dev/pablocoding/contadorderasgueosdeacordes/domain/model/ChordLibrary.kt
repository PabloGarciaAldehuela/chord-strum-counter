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
        ),

        // Stuck 3&4 Chords (JustinGuitar Grade 2)
        Chord(
            name = "Big G",
            fullName = "G Major (Four Finger / Big G)",
            frets = listOf(3, 2, 0, 0, 3, 3),
            fingers = listOf(2, 1, 0, 0, 3, 4)
        ),
        Chord(
            name = "G5",
            fullName = "G5 (The Rock G)",
            frets = listOf(3, -1, 0, 0, 3, 3),
            fingers = listOf(2, 0, 0, 0, 3, 4)
        ),
        Chord(
            name = "Cadd9",
            fullName = "C Added 9th",
            frets = listOf(-1, 3, 2, 0, 3, 3),
            fingers = listOf(0, 2, 1, 0, 3, 4)
        ),
        Chord(
            name = "Dsus4",
            fullName = "D Suspended 4th",
            frets = listOf(-1, -1, 0, 2, 3, 3),
            fingers = listOf(0, 0, 0, 1, 3, 4)
        ),
        Chord(
            name = "A7sus4",
            fullName = "A7 Suspended 4th",
            frets = listOf(-1, 0, 2, 2, 3, 3),
            fingers = listOf(0, 0, 1, 2, 3, 4)
        ),
        Chord(
            name = "A7sus4 (var)",
            fullName = "A7 Suspended 4th (Open G / Wonderwall)",
            frets = listOf(-1, 0, 2, 0, 3, 3),
            fingers = listOf(0, 0, 1, 0, 3, 4)
        ),
        Chord(
            name = "Em7",
            fullName = "E Minor 7th (Stuck 3&4)",
            frets = listOf(0, 2, 2, 0, 3, 3),
            fingers = listOf(0, 1, 2, 0, 3, 4)
        ),
        Chord(
            name = "Dadd11/F#",
            fullName = "D add11 over F#",
            frets = listOf(2, -1, 0, 2, 3, 3),
            fingers = listOf(1, 0, 0, 2, 3, 4)
        ),
        Chord(
            name = "F6/9",
            fullName = "F Major 6/9 (Stuck 3&4)",
            frets = listOf(-1, -1, 3, 2, 3, 3),
            fingers = listOf(0, 0, 2, 1, 3, 4)
        )
    )

    private val aliases: Map<String, String> = mapOf(
        "C ADD9" to "CADD9",
        "D SUS4" to "DSUS4",
        "A7 SUS4" to "A7SUS4",
        "A7 SUS 4" to "A7SUS4",
        "A7SUS4(VAR)" to "A7SUS4 (VAR)",
        "A7SUS4 VAR" to "A7SUS4 (VAR)",
        "A7 SUS 4 (VARIATION)" to "A7SUS4 (VAR)",
        "A7SUS4 (OPEN G)" to "A7SUS4 (VAR)",
        "E MIN7" to "EM7",
        "EMIN7" to "EM7",
        "D ADD11/F#" to "DADD11/F#",
        "F 6/9" to "F6/9",
        "ROCK G" to "G5",
        "ROCK G5" to "G5",
        "FOUR FINGER G" to "BIG G",
        "G (BIG G)" to "BIG G",
        "G (BIG)" to "BIG G"
    )

    private val chordMap: Map<String, Chord> by lazy {
        allChords.associateBy { it.name.uppercase() }
    }

    fun getChord(name: String): Chord? {
        val trimmedUpper = name.trim().uppercase()
        val resolvedKey = aliases[trimmedUpper] ?: trimmedUpper
        return chordMap[resolvedKey]
    }
}
