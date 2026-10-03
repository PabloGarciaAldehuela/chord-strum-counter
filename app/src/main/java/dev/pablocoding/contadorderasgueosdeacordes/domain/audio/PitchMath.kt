package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Equal-temperament helpers. Notes are MIDI numbers (A4 = 69, E2 = 40, C-1 = 0) and all target
 * frequencies derive from a configurable reference pitch for A4.
 */
object PitchMath {

    const val A4_MIDI = 69
    const val DEFAULT_REFERENCE_HZ = 440.0

    private val SHARP_LETTERS = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val FLAT_LETTERS = listOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")
    private val SHARP_SOLFEGE = listOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    private val FLAT_SOLFEGE = listOf("Do", "Re♭", "Re", "Mi♭", "Mi", "Fa", "Sol♭", "Sol", "La♭", "La", "Si♭", "Si")
    private val LETTER_PITCH_CLASS = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
    private val LN2 = ln(2.0)

    fun midiToFrequency(midi: Int, referenceHz: Double = DEFAULT_REFERENCE_HZ): Double =
        referenceHz * 2.0.pow((midi - A4_MIDI) / 12.0)

    /** Fractional MIDI number for a frequency (e.g. 40.05 = E2 five cents sharp). */
    fun frequencyToMidi(frequencyHz: Double, referenceHz: Double = DEFAULT_REFERENCE_HZ): Double =
        A4_MIDI + 12.0 * ln(frequencyHz / referenceHz) / LN2

    fun nearestMidi(frequencyHz: Double, referenceHz: Double = DEFAULT_REFERENCE_HZ): Int =
        frequencyToMidi(frequencyHz, referenceHz).roundToInt()

    /** Positive = sharp (tune down), negative = flat (tune up). */
    fun cents(frequencyHz: Double, targetHz: Double): Double = 1200.0 * ln(frequencyHz / targetHz) / LN2

    fun octave(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun noteName(midi: Int, preferFlats: Boolean = false, naming: NoteNaming = NoteNaming.LETTERS): String {
        val pitchClass = Math.floorMod(midi, 12)
        val names = when (naming) {
            NoteNaming.LETTERS -> if (preferFlats) FLAT_LETTERS else SHARP_LETTERS
            NoteNaming.SOLFEGE -> if (preferFlats) FLAT_SOLFEGE else SHARP_SOLFEGE
        }
        return names[pitchClass]
    }

    /**
     * Parses scientific pitch notation such as "E2", "C#3", "Eb4" or "B1" into a MIDI number.
     */
    fun parseNote(note: String): Int {
        val trimmed = note.trim()
        require(trimmed.length >= 2) { "Invalid note '$note'" }
        val base = LETTER_PITCH_CLASS[trimmed[0].uppercaseChar()] ?: throw IllegalArgumentException("Invalid note '$note'")
        var index = 1
        var accidental = 0
        while (index < trimmed.length && trimmed[index] in "#♯b♭") {
            accidental += if (trimmed[index] == '#' || trimmed[index] == '♯') 1 else -1
            index++
        }
        val octave = trimmed.substring(index).toIntOrNull() ?: throw IllegalArgumentException("Invalid note '$note'")
        return (octave + 1) * 12 + base + accidental
    }
}
