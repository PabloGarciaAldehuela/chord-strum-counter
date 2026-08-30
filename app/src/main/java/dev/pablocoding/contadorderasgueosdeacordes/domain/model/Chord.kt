package dev.pablocoding.contadorderasgueosdeacordes.domain.model

/**
 * Represents a guitar chord for visual diagram rendering and practice progressions.
 *
 * @param name Short chord symbol (e.g. "A", "Em", "D7", "C")
 * @param fullName Descriptive name (e.g. "A Major", "E Minor", "D Dominant 7th")
 * @param frets Fret positions from string 6 (low E) down to string 1 (high e):
 *              -1 = muted ('X'), 0 = open ('O'), 1..5 = fret number
 * @param fingers Recommended finger number for each string:
 *                0 = none, 1 = index, 2 = middle, 3 = ring, 4 = pinky
 * @param baseFret The top fret displayed in the diagram (default 1)
 */
data class Chord(
    val name: String,
    val fullName: String,
    val frets: List<Int>,
    val fingers: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val baseFret: Int = 1
)
