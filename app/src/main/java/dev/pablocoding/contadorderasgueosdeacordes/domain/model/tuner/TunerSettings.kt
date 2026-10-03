package dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner

enum class NoteNaming(val label: String) {
    LETTERS("A B C"),
    SOLFEGE("Do Re Mi");

    companion object {
        private val SOLFEGE_LANGUAGES = setOf("es", "it", "fr", "pt", "ca", "gl", "ro")

        /** Fixed-do (Do Re Mi) for languages where it's the common convention, letters otherwise. */
        fun defaultForLanguage(languageCode: String): NoteNaming =
            if (languageCode.lowercase() in SOLFEGE_LANGUAGES) SOLFEGE else LETTERS
    }
}

/**
 * How close (in cents) a string must be to count as in tune. Leaving the in-tune state requires
 * drifting [EXIT_MARGIN_CENTS] further, so the indicator doesn't flicker at the boundary.
 */
enum class TunerTolerance(val cents: Double, val label: String) {
    STRICT(2.0, "Strict\n±2¢"),
    NORMAL(3.0, "Normal\n±3¢"),
    RELAXED(5.0, "Relaxed\n±5¢");

    companion object {
        const val EXIT_MARGIN_CENTS = 2.0
    }
}

/**
 * Which part of a plucked note the tuner reads. A plucked string starts a few cents sharp and sinks
 * as it rings (measured on real strings: 5–10¢ over the first second), so the choice matters.
 */
enum class TunerReadingMode(val label: String) {
    /** The needle follows the note from the pluck on, attack included. */
    LIVE("Live\nfrom the pluck"),
    /** One steady reading of the ringing note (its early sustain, after the attack has passed). */
    STEADY("Steady\nringing note");
}

data class TunerSettings(
    val tuningId: String = TuningLibrary.STANDARD_ID,
    val referencePitchHz: Int = DEFAULT_REFERENCE_PITCH_HZ,
    val inTuneSound: Boolean = true,
    val leftHanded: Boolean = false,
    val noteNaming: NoteNaming = NoteNaming.LETTERS,
    val tolerance: TunerTolerance = TunerTolerance.NORMAL,
    val readingMode: TunerReadingMode = TunerReadingMode.LIVE
) {
    companion object {
        const val MIN_REFERENCE_PITCH_HZ = 430
        const val MAX_REFERENCE_PITCH_HZ = 450
        const val DEFAULT_REFERENCE_PITCH_HZ = 440
    }
}
