package dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner

sealed interface TunerError {
    data object MicrophonePermissionDenied : TunerError
    data object MicrophoneUnavailable : TunerError
    data object RecordingFailed : TunerError
    data object PracticeSessionActive : TunerError
}

/**
 * Evaluated tuner output for one analysis hop.
 *
 * @param frequencyHz Stable detected pitch, null when nothing is ringing.
 * @param detectedMidi Nearest semitone to the detected pitch.
 * @param targetMidi Note the user should tune to (target string, or nearest semitone in chromatic mode).
 * @param targetStringIndex Index into [Tuning.strings], null in chromatic mode.
 * @param cents Deviation from [targetMidi]; negative = flat (tune up), positive = sharp (tune down).
 * @param justBecameInTune True only on the hop where the in-tune state was entered.
 * @param showNoiseHint Signal present but unusable for several seconds (noisy room, chords).
 */
data class TunerReading(
    val frequencyHz: Double? = null,
    val detectedMidi: Int? = null,
    val targetMidi: Int? = null,
    val targetStringIndex: Int? = null,
    val cents: Double? = null,
    val isInTune: Boolean = false,
    val justBecameInTune: Boolean = false,
    val isHolding: Boolean = false,
    val signalLevel: Double = 0.0,
    val showNoiseHint: Boolean = false,
    val isInputSilenced: Boolean = false,
    val timestampMs: Long = 0L
) {
    val hasPitch: Boolean get() = frequencyHz != null && cents != null
}
