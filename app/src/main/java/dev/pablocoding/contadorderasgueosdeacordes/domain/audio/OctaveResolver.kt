package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs

/**
 * Fixes octave (and twelfth) errors using the tuning as a prior.
 *
 * Phone microphones barely capture a low string's fundamental, so a string whose odd harmonics
 * are weak looks periodic at twice its pitch (real capture: the A string read as 224 Hz), and a
 * small NSDF imbalance can pick twice the period (the B string read as 123 Hz). When an estimate
 * lands far from every string but ×½, ×2, ×⅓ or ×3 of it lands on one, and the signal is also
 * periodic at that alternative, the alternative is the real note.
 */
object OctaveResolver {

    /** An estimate this close to a string is trusted as-is. */
    const val TRUSTED_CENTS = 70.0
    /** An alternative must land within 200 cents (2 semitones) of a string. */
    const val ALTERNATIVE_CENTS = 200.0
    /** An alternative's periodicity must be at least this fraction of the original's. */
    const val MIN_RELATIVE_SUPPORT = 0.6

    private val RATIOS = doubleArrayOf(0.5, 2.0, 1.0 / 3.0, 3.0)

    /**
     * @param support Periodicity of the signal at a given pitch (e.g. [MpmPitchDetector.supportAt]).
     */
    fun resolve(frequencyHz: Double, targetsHz: List<Double>, support: (Double) -> Double): Double {
        if (targetsHz.isEmpty()) return frequencyHz
        val originalDistance = distanceToNearest(frequencyHz, targetsHz)
        if (originalDistance <= TRUSTED_CENTS) return frequencyHz
        val reference = support(frequencyHz).coerceAtLeast(1e-9)
        var best = frequencyHz
        var bestDistance = originalDistance
        for (ratio in RATIOS) {
            val candidate = frequencyHz * ratio
            val distance = distanceToNearest(candidate, targetsHz)
            if (distance <= ALTERNATIVE_CENTS && distance < bestDistance && support(candidate) >= MIN_RELATIVE_SUPPORT * reference) {
                best = candidate
                bestDistance = distance
            }
        }
        return best
    }

    fun distanceToNearest(frequencyHz: Double, targetsHz: List<Double>): Double =
        targetsHz.minOf { abs(PitchMath.cents(frequencyHz, it)) }
}
