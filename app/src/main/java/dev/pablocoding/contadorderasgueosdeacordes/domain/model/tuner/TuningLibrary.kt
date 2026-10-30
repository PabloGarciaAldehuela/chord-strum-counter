package dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath

/**
 * Built-in guitar tunings. Notes are listed low (string 6) to high (string 1).
 */
object TuningLibrary {

    const val STANDARD_ID = "standard"
    const val CHROMATIC_ID = "chromatic"

    private fun tuning(
        id: String,
        name: String,
        category: TuningCategory,
        notes: String,
        preferFlats: Boolean = false
    ) = Tuning(id, name, category, notes.split(" ").map(PitchMath::parseNote), preferFlats)

    val standard = tuning(STANDARD_ID, "Standard", TuningCategory.STANDARD, "E2 A2 D3 G3 B3 E4")

    val chromatic = Tuning(CHROMATIC_ID, "Chromatic", TuningCategory.CHROMATIC, emptyList())

    val allTunings: List<Tuning> = listOf(
        standard,
        tuning("drop_d", "Drop D", TuningCategory.DROP, "D2 A2 D3 G3 B3 E4"),
        tuning("drop_c_sharp", "Drop C♯", TuningCategory.DROP, "C#2 G#2 C#3 F#3 A#3 D#4"),
        tuning("drop_c", "Drop C", TuningCategory.DROP, "C2 G2 C3 F3 A3 D4"),
        tuning("drop_b", "Drop B", TuningCategory.DROP, "B1 F#2 B2 E3 G#3 C#4"),
        tuning("drop_a", "Drop A", TuningCategory.DROP, "A1 E2 A2 D3 F#3 B3"),
        tuning("half_step_down", "Half Step Down (E♭)", TuningCategory.LOWERED, "Eb2 Ab2 Db3 Gb3 Bb3 Eb4", preferFlats = true),
        tuning("full_step_down", "Full Step Down (D)", TuningCategory.LOWERED, "D2 G2 C3 F3 A3 D4"),
        tuning("open_d", "Open D", TuningCategory.OPEN, "D2 A2 D3 F#3 A3 D4"),
        tuning("open_g", "Open G", TuningCategory.OPEN, "D2 G2 D3 G3 B3 D4"),
        tuning("open_e", "Open E", TuningCategory.OPEN, "E2 B2 E3 G#3 B3 E4"),
        tuning("open_a", "Open A", TuningCategory.OPEN, "E2 A2 E3 A3 C#4 E4"),
        tuning("open_c", "Open C", TuningCategory.OPEN, "C2 G2 C3 G3 C4 E4"),
        tuning("dadgad", "DADGAD", TuningCategory.ALTERNATE, "D2 A2 D3 G3 A3 D4"),
        chromatic
    )

    /** Unknown ids (e.g. a tuning removed in a later version) fall back to Standard. */
    fun getTuning(id: String): Tuning = allTunings.firstOrNull { it.id == id } ?: standard
}
