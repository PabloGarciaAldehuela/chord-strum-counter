package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import kotlin.math.pow

/**
 * What the pitch pipeline should listen for: the current tuning's string frequencies (already
 * scaled to the reference pitch) and, in manual mode, the locked string.
 *
 * Knowing the targets lets the pipeline restrict its search range, resolve octave ambiguities,
 * band-limit the input and run the per-string harmonic analysis. An empty [targetsHz] means the
 * chromatic tuner: any note, full range, no string priors. [readingMode] picks which part of each
 * note is reported (see [TunerReadingMode]).
 */
data class TunerAnalysisContext(
    val targetsHz: List<Double> = emptyList(),
    val lockedIndex: Int? = null,
    val readingMode: TunerReadingMode = TunerReadingMode.LIVE
) {
    val isChromatic: Boolean get() = targetsHz.isEmpty()

    /** Lowest and highest fundamental worth reporting. */
    val searchRangeHz: ClosedFloatingPointRange<Double>
        get() = if (isChromatic) CHROMATIC_MIN_HZ..CHROMATIC_MAX_HZ
        else targetsHz.min() * SEMITONES_7_DOWN..targetsHz.max() * SEMITONES_7_UP

    /** High-pass corner just below the lowest string (room rumble and mains hum live below it). */
    val highPassHz: Double
        get() = if (isChromatic) CHROMATIC_HIGH_PASS_HZ else (targetsHz.min() * 0.7).coerceAtLeast(MIN_HIGH_PASS_HZ)

    companion object {
        const val CHROMATIC_MIN_HZ = 50.0
        const val CHROMATIC_MAX_HZ = 1400.0
        const val CHROMATIC_HIGH_PASS_HZ = 40.0
        const val MIN_HIGH_PASS_HZ = 35.0
        private val SEMITONES_7_DOWN = 2.0.pow(-7 / 12.0)
        private val SEMITONES_7_UP = 2.0.pow(7 / 12.0)

        val CHROMATIC = TunerAnalysisContext()
    }
}
