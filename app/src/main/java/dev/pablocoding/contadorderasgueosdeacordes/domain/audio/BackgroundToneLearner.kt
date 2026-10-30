package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs

/**
 * Learns steady background tones (mains hum from an amp or lights, a fridge compressor, a fan
 * motor) while no note is being played, so the pipeline can notch them out. On the reference
 * Pixel capture a ~58 Hz tone sat at the same level as the strings' harmonics for the whole take.
 *
 * A tone qualifies when, while idle, one pitch persists with clarity ≥ [minClarity] and drifts no
 * more than [toleranceCents] for [stableMs]. Pitches near any string harmonic are never learned, so
 * a string ringing on after a note can't notch itself out.
 */
class BackgroundToneLearner(
    private val stableMs: Long = 1000,
    private val minClarity: Double = 0.8,
    private val toleranceCents: Double = 10.0,
    private val protectCents: Double = 30.0,
    private val maxTones: Int = 2
) {
    private var candidateHz = 0.0
    private var candidateSinceMs = Long.MIN_VALUE
    private val learned = ArrayList<Double>()

    val tones: List<Double> get() = learned

    /**
     * @param estimate Raw periodicity estimate of the idle signal.
     * @param targetsHz Current string targets (harmonics up to the 8th are protected).
     * @return A newly learned tone frequency, or null.
     */
    fun observe(estimate: PitchEstimate?, nowMs: Long, idle: Boolean, targetsHz: List<Double>): Double? {
        if (!idle || learned.size >= maxTones || estimate == null || estimate.clarity < minClarity) {
            candidateSinceMs = Long.MIN_VALUE
            return null
        }
        val f = estimate.frequencyHz
        if (isProtected(f, targetsHz) || learned.any { abs(PitchMath.cents(f, it)) < 50 }) {
            candidateSinceMs = Long.MIN_VALUE
            return null
        }
        if (candidateSinceMs == Long.MIN_VALUE || abs(PitchMath.cents(f, candidateHz)) > toleranceCents) {
            candidateHz = f
            candidateSinceMs = nowMs
            return null
        }
        candidateHz = 0.5 * (candidateHz + f)
        if (nowMs - candidateSinceMs < stableMs) return null
        learned.add(candidateHz)
        candidateSinceMs = Long.MIN_VALUE
        return candidateHz
    }

    /** Harmonics of [toneHz] to notch: below [maxHz] and not on top of any string harmonic. */
    fun notchFrequencies(toneHz: Double, targetsHz: List<Double>, maxHz: Double): List<Double> =
        generateSequence(toneHz) { it + toneHz }
            .takeWhile { it < maxHz }
            .filterNot { isProtected(it, targetsHz) }
            .toList()

    fun reset() {
        learned.clear()
        candidateSinceMs = Long.MIN_VALUE
    }

    private fun isProtected(frequencyHz: Double, targetsHz: List<Double>): Boolean =
        targetsHz.any { target -> (1..8).any { h -> abs(PitchMath.cents(frequencyHz, target * h)) < protectCents } }
}
