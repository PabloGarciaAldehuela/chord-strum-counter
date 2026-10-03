package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

/**
 * Raw single-frame pitch estimate.
 *
 * @param frequencyHz Estimated fundamental frequency.
 * @param clarity Periodicity confidence in [0, 1]; ~1.0 for a clean tone, low for noise or chords.
 */
data class PitchEstimate(
    val frequencyHz: Double,
    val clarity: Double
)

/**
 * Estimates the fundamental frequency of one analysis window of mono PCM audio in [-1, 1].
 */
interface PitchDetector {
    val windowSize: Int

    /** Returns null when no periodic signal is found in the window. */
    fun detect(frame: FloatArray): PitchEstimate?
}
