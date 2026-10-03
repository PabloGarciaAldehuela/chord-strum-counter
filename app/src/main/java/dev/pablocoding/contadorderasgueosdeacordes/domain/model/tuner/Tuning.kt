package dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner

enum class TuningCategory(val label: String) {
    STANDARD("Standard"),
    DROP("Drop"),
    LOWERED("Lowered"),
    OPEN("Open"),
    ALTERNATE("Alternate"),
    CHROMATIC("Chromatic")
}

/**
 * A guitar tuning.
 *
 * @param strings Target MIDI notes ordered like [dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord.frets]:
 *                index 0 = string 6 (lowest), last index = string 1 (highest).
 *                Empty for the chromatic tuner, which targets the nearest semitone.
 * @param preferFlats Spell accidentals as flats (E♭ standard) instead of sharps.
 */
data class Tuning(
    val id: String,
    val name: String,
    val category: TuningCategory,
    val strings: List<Int>,
    val preferFlats: Boolean = false
) {
    val isChromatic: Boolean get() = strings.isEmpty()

    /** Guitar string number (6 = lowest) for a string index. */
    fun stringNumber(index: Int): Int = strings.size - index
}
