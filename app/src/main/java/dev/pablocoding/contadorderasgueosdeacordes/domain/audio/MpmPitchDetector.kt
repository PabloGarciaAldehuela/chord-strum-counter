package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * McLeod Pitch Method (MPM) detector.
 *
 * 1. Computes the Normalized Square Difference Function (NSDF) using FFT-based autocorrelation.
 * 2. Picks the first "key maximum" whose height is at least [cutoff] × the highest key maximum,
 *    which is what protects against octave errors on harmonic-rich low strings.
 * 3. Refines the chosen lag with parabolic interpolation. At 48 kHz one sample of lag at low E
 *    is ~3 cents, so sub-sample precision is required for ±1 cent accuracy.
 *
 * Time-domain periodicity (unlike FFT peak picking) still finds the true period when a phone
 * microphone barely captures the fundamental of a low string.
 *
 * Pure Kotlin with no per-frame allocations, so it is JVM-testable and safe to call ~50×/second.
 */
class MpmPitchDetector(
    val sampleRate: Int,
    override val windowSize: Int,
    minFrequencyHz: Double = DEFAULT_MIN_FREQUENCY_HZ,
    maxFrequencyHz: Double = DEFAULT_MAX_FREQUENCY_HZ,
    private val cutoff: Double = DEFAULT_CUTOFF
) : PitchDetector {

    companion object {
        const val DEFAULT_MIN_FREQUENCY_HZ = 50.0
        const val DEFAULT_MAX_FREQUENCY_HZ = 1400.0
        const val DEFAULT_CUTOFF = 0.93
        private const val MIN_ENERGY = 1e-10
    }

    private val minLag = max(2, floor(sampleRate / maxFrequencyHz).toInt())
    private val maxLag = min(windowSize - 2, ceil(sampleRate / minFrequencyHz).toInt() + 2)

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(maxLag > minLag) { "windowSize $windowSize too small for ${minFrequencyHz}Hz at ${sampleRate}Hz" }
    }

    private val fft = Fft(nextPowerOfTwo(windowSize * 2))
    private val re = DoubleArray(fft.size)
    private val im = DoubleArray(fft.size)
    private val samples = DoubleArray(windowSize)
    private val nsdf = DoubleArray(maxLag + 2)
    private val keyMaxima = IntArray(maxLag + 2)

    override fun detect(frame: FloatArray): PitchEstimate? {
        require(frame.size >= windowSize) { "Frame must hold at least $windowSize samples" }

        var mean = 0.0
        for (i in 0 until windowSize) mean += frame[i]
        mean /= windowSize

        var energy = 0.0
        for (i in 0 until windowSize) {
            val x = frame[i] - mean
            samples[i] = x
            energy += x * x
        }
        if (energy < MIN_ENERGY) return null

        computeNsdf(energy)
        val keyCount = findKeyMaxima()
        if (keyCount == 0) return null

        var highest = 0.0
        for (k in 0 until keyCount) highest = max(highest, nsdf[keyMaxima[k]])
        if (highest <= 0.0) return null

        val threshold = cutoff * highest
        var chosen = -1
        for (k in 0 until keyCount) {
            if (nsdf[keyMaxima[k]] >= threshold) {
                chosen = keyMaxima[k]
                break
            }
        }
        if (chosen < 0) return null

        val a = nsdf[chosen - 1]
        val b = nsdf[chosen]
        val c = nsdf[chosen + 1]
        val denominator = a - 2.0 * b + c
        val delta = if (abs(denominator) < 1e-12) 0.0 else (0.5 * (a - c) / denominator).coerceIn(-1.0, 1.0)
        val period = chosen + delta
        val peak = b - 0.25 * (a - c) * delta

        return PitchEstimate(
            frequencyHz = sampleRate / period,
            clarity = peak.coerceIn(0.0, 1.0)
        )
    }

    /**
     * Periodicity support for an alternative pitch, from the NSDF of the last [detect] call: the
     * highest NSDF value within ±3% of that pitch's lag (0 when the lag is outside the computed range).
     * Used to verify octave corrections — a real alternative must also be periodic in the signal.
     */
    fun supportAt(frequencyHz: Double): Double {
        if (frequencyHz <= 0.0) return 0.0
        val lag = sampleRate / frequencyHz
        val from = max(1, floor(lag * 0.97).toInt())
        val to = min(maxLag + 1, ceil(lag * 1.03).toInt())
        if (from > to) return 0.0
        var best = 0.0
        for (tau in from..to) best = max(best, nsdf[tau])
        return best
    }

    /**
     * NSDF n(τ) = 2·r(τ) / m(τ), where r is the autocorrelation (computed via FFT) and
     * m(τ) = Σ (x_j² + x_{j+τ}²) over the overlapping region, updated incrementally.
     */
    private fun computeNsdf(energy: Double) {
        re.fill(0.0)
        im.fill(0.0)
        System.arraycopy(samples, 0, re, 0, windowSize)
        fft.forward(re, im)
        for (k in 0 until fft.size) {
            re[k] = re[k] * re[k] + im[k] * im[k]
            im[k] = 0.0
        }
        fft.inverse(re, im)

        var m = 2.0 * energy
        for (tau in 0..maxLag + 1) {
            nsdf[tau] = if (m > MIN_ENERGY) 2.0 * re[tau] / m else 0.0
            val head = samples[tau]
            val tail = samples[windowSize - 1 - tau]
            m -= head * head + tail * tail
        }
    }

    /**
     * Collects the highest lag of each positive NSDF lobe after the first negative-going zero
     * crossing (the lobe at τ = 0 is always 1 and carries no pitch information).
     */
    private fun findKeyMaxima(): Int {
        var count = 0
        var tau = 1
        while (tau <= maxLag && nsdf[tau] > 0.0) tau++

        var inLobe = false
        var lobeMax = -1
        while (tau <= maxLag) {
            val value = nsdf[tau]
            if (value > 0.0) {
                if (!inLobe) {
                    inLobe = true
                    lobeMax = tau
                } else if (value > nsdf[lobeMax]) {
                    lobeMax = tau
                }
            } else if (inLobe) {
                if (lobeMax >= minLag) keyMaxima[count++] = lobeMax
                inLobe = false
            }
            tau++
        }
        // A lobe cut off by the search limit only counts if its peak is clearly inside the range.
        if (inLobe && lobeMax in minLag until maxLag) keyMaxima[count++] = lobeMax
        return count
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }
}
