package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * One string's harmonic evidence in the current analysis window.
 *
 * @param score Sum of the agreeing harmonics' SNRs (dB above local noise); higher = more of this string.
 * @param harmonics Number of harmonics that agree on [frequencyHz].
 */
data class StringCandidate(
    val index: Int,
    val frequencyHz: Double,
    val score: Double,
    val harmonics: Int,
    /** Harmonic numbers that contributed, and their SNRs (dB), for shared-harmonic arbitration. */
    val harmonicNumbers: IntArray = IntArray(0),
    val harmonicSnrs: DoubleArray = DoubleArray(0)
) {
    /** Score without the harmonics that are multiples of [m] (shared with a string m× higher). */
    fun scoreExcludingMultiplesOf(m: Int): Double {
        var total = 0.0
        for (i in harmonicNumbers.indices) if (harmonicNumbers[i] % m != 0) total += harmonicSnrs[i]
        return total
    }

    override fun equals(other: Any?) = other is StringCandidate && index == other.index &&
        frequencyHz == other.frequencyHz && score == other.score && harmonics == other.harmonics
    override fun hashCode() = 31 * index + frequencyHz.hashCode()
}

/**
 * Frequency-domain, tuning-aware pitch detector for weak or mixed signals.
 *
 * Time-domain MPM needs the string to dominate the whole band; on a real phone an unplugged
 * electric's thin strings sit *below* the room's broadband noise (measured clarity 0.3–0.7), yet in
 * a long FFT their harmonics stand 14–26 dB above the local noise. This detector exploits that
 * narrowband processing gain:
 *
 * 1. A ~340 ms Hann-windowed, 2× zero-padded FFT.
 * 2. For every string target, the strongest peak within ±[searchCents] of each harmonic h·f
 *    (h ≤ 8, below [maxHarmonicHz]) with its SNR against the median of the surrounding ±30 Hz;
 *    only peaks ≥ [minSnrDb] count.
 * 3. Harmonics whose implied fundamentals agree within [agreeCents] form the string's evidence;
 *    a string needs ≥ 2 such harmonics, the lowest no higher than the 3rd (so a high string's
 *    partials can't be mistaken for a low string's upper harmonics).
 * 4. The pitch is a power-weighted mean of peak/h over the first [pitchHarmonics] harmonics:
 *    stiff wound strings' upper partials run increasingly sharp (and are loudest just after the
 *    pluck), so only the lower ones give a stable reading. Peaks use Gaussian interpolation of
 *    the Hann main lobe.
 *
 * The ±[searchCents] windows around each string make octave confusion impossible by construction.
 */
class HarmonicSpectrumDetector(
    val sampleRate: Int,
    val windowSize: Int = windowSizeFor(sampleRate),
    private val searchCents: Double = 150.0,
    private val agreeCents: Double = 15.0,
    /**
     * Per-harmonic SNR bar. Noise magnitudes are Rayleigh-distributed: within a ±150¢ search window
     * the largest noise bin reaches ~8 dB above the median a few percent of the time, but 10 dB only
     * ~1 in 1000 bins, so two agreeing harmonics at ≥ 10 dB are very unlikely to be noise.
     */
    private val minSnrDb: Double = 10.0,
    private val minScore: Double = 24.0,
    private val maxHarmonics: Int = 10,
    private val maxHarmonicHz: Double = 1500.0,
    /** Harmonics used for the pitch value (more may still count as evidence for the string). */
    private val pitchHarmonics: Int = 6
) {
    companion object {
        private const val WINDOW_SECONDS = 0.3
        private const val NOISE_HALF_WIDTH_HZ = 30.0
        private const val PEAK_EXCLUSION_BINS = 4

        /** Power of two covering ~300 ms (16384 samples at 44.1/48 kHz). */
        fun windowSizeFor(sampleRate: Int): Int {
            val target = (sampleRate * WINDOW_SECONDS).toInt()
            var size = 2048
            while (size < target) size = size shl 1
            return size
        }
    }

    private val fftSize = windowSize * 2
    private val fft = Fft(fftSize)
    private val re = DoubleArray(fftSize)
    private val im = DoubleArray(fftSize)
    private val magnitude = DoubleArray(fftSize / 2)
    private val hann = DoubleArray(windowSize) { 0.5 - 0.5 * cos(2.0 * PI * it / (windowSize - 1)) }
    private val binHz = sampleRate.toDouble() / fftSize
    private val noiseScratch = DoubleArray((2 * NOISE_HALF_WIDTH_HZ / binHz).toInt() + 8)
    private val searchUp = 2.0.pow(searchCents / 1200.0)
    private val searchDown = 2.0.pow(-searchCents / 1200.0)

    // Per-harmonic scratch, reused across calls.
    private val hNumber = IntArray(maxHarmonics)
    private val hFreq = DoubleArray(maxHarmonics)
    private val hSnr = DoubleArray(maxHarmonics)
    private val hCents = DoubleArray(maxHarmonics)

    /**
     * Analyses one window and returns a candidate for every string with enough harmonic evidence.
     */
    fun analyze(frame: FloatArray, targetsHz: List<Double>): List<StringCandidate> {
        require(frame.size >= windowSize) { "Frame must hold at least $windowSize samples" }
        if (targetsHz.isEmpty()) return emptyList()
        transform(frame)
        val result = ArrayList<StringCandidate>(targetsHz.size)
        for ((index, target) in targetsHz.withIndex()) {
            evaluateString(index, target)?.let(result::add)
        }
        return result
    }

    private fun transform(frame: FloatArray) {
        var mean = 0.0
        for (i in 0 until windowSize) mean += frame[i]
        mean /= windowSize
        re.fill(0.0)
        im.fill(0.0)
        for (i in 0 until windowSize) re[i] = (frame[i] - mean) * hann[i]
        fft.forward(re, im)
        for (k in magnitude.indices) magnitude[k] = sqrt(re[k] * re[k] + im[k] * im[k])
    }

    private fun evaluateString(index: Int, target: Double): StringCandidate? {
        var count = 0
        for (h in 1..maxHarmonics) {
            val center = target * h
            if (center * searchUp > maxHarmonicHz) break
            val lo = max(2, (center * searchDown / binHz).toInt())
            val hi = min(magnitude.size - 3, (center * searchUp / binHz).toInt() + 1)
            if (hi - lo < 3) continue
            var peak = lo
            for (k in lo..hi) if (magnitude[k] > magnitude[peak]) peak = k
            // A maximum on the search boundary is the skirt of something outside this window.
            if (peak == lo || peak == hi) continue
            val snr = snrDb(peak)
            if (snr < minSnrDb) continue
            val frequency = interpolatedFrequency(peak)
            hNumber[count] = h
            hFreq[count] = frequency
            hSnr[count] = snr
            hCents[count] = PitchMath.cents(frequency / h, target)
            count++
        }
        if (count < 2) return null

        // Largest group of harmonics agreeing on one fundamental (weighted by SNR).
        var bestPivot = -1
        var bestWeight = 0.0
        for (p in 0 until count) {
            var weight = 0.0
            var members = 0
            for (j in 0 until count) {
                if (abs(hCents[j] - hCents[p]) <= agreeCents) {
                    weight += hSnr[j]
                    members++
                }
            }
            if (members >= 2 && weight > bestWeight) {
                bestWeight = weight
                bestPivot = p
            }
        }
        if (bestPivot < 0) return null

        // Consensus deviation of the agreeing group (SNR-weighted).
        var consensusWeight = 0.0
        var consensusCents = 0.0
        for (j in 0 until count) {
            if (abs(hCents[j] - hCents[bestPivot]) > agreeCents) continue
            consensusWeight += hSnr[j]
            consensusCents += hSnr[j] * hCents[j]
        }
        consensusCents /= consensusWeight

        // Second pass: re-pick every harmonic as the local peak *nearest* the consensus position.
        // The strongest peak in a ±150¢ window can be a neighbouring string's overtone (strings
        // plucked earlier keep ringing); the nearest one is this string's own partial.
        var members = 0
        var lowest = Int.MAX_VALUE
        var weightSum = 0.0
        var logSum = 0.0
        val numbers = IntArray(maxHarmonics)
        val snrs = DoubleArray(maxHarmonics)
        val consensusFactor = 2.0.pow(consensusCents / 1200.0)
        for (h in 1..maxHarmonics) {
            val expected = target * h * consensusFactor
            if (expected * 2.0.pow(agreeCents / 1200.0) > maxHarmonicHz) break
            val peak = nearestPeak(expected) ?: continue
            val snr = snrDb(peak)
            if (snr < minSnrDb) continue
            val frequency = interpolatedFrequency(peak)
            if (abs(PitchMath.cents(frequency, expected)) > agreeCents) continue
            numbers[members] = h
            snrs[members] = snr
            members++
            lowest = min(lowest, h)
            // Weighted by power over the lower harmonics only (see [pitchHarmonics]): on a stiff
            // wound string the upper partials run increasingly sharp and are loudest right after
            // the pluck, so including them makes the reading swing sharp at every pluck (measured:
            // +10–12¢ on a real low E) and drift as they fade. The lower harmonics agree within ~3¢.
            if (h > pitchHarmonics) continue
            val weight = magnitude[peak] * magnitude[peak]
            weightSum += weight
            logSum += weight * ln(frequency / h)
        }
        if (members < 2 || lowest > 3 || weightSum <= 0.0 || bestWeight < minScore) return null
        return StringCandidate(
            index, kotlin.math.exp(logSum / weightSum), bestWeight, members,
            numbers.copyOf(members), snrs.copyOf(members)
        )
    }

    /** Local maximum closest to [frequencyHz] within ±[agreeCents], or null. */
    private fun nearestPeak(frequencyHz: Double): Int? {
        val center = (frequencyHz / binHz).toInt()
        val reach = max(2, (frequencyHz * (2.0.pow(agreeCents / 1200.0) - 1.0) / binHz).toInt() + 1)
        for (offset in 0..reach) {
            for (k in intArrayOf(center - offset, center + offset)) {
                if (k < 2 || k >= magnitude.size - 2) continue
                if (magnitude[k] > magnitude[k - 1] && magnitude[k] >= magnitude[k + 1]) return k
            }
        }
        return null
    }

    /** Peak magnitude relative to the median magnitude of the surrounding ±30 Hz (peak excluded), in dB. */
    private fun snrDb(peak: Int): Double {
        val halfWidth = (NOISE_HALF_WIDTH_HZ / binHz).toInt()
        var n = 0
        for (k in max(1, peak - halfWidth)..min(magnitude.size - 1, peak + halfWidth)) {
            if (abs(k - peak) <= PEAK_EXCLUSION_BINS) continue
            noiseScratch[n++] = magnitude[k]
        }
        if (n == 0) return 0.0
        noiseScratch.sort(0, n)
        val noise = noiseScratch[n / 2].coerceAtLeast(1e-15)
        return 20.0 * log10(magnitude[peak] / noise)
    }

    /** Gaussian (log-parabolic) interpolation — near-exact for a Hann-windowed sinusoid. */
    private fun interpolatedFrequency(peak: Int): Double {
        val a = ln(magnitude[peak - 1].coerceAtLeast(1e-15))
        val b = ln(magnitude[peak].coerceAtLeast(1e-15))
        val c = ln(magnitude[peak + 1].coerceAtLeast(1e-15))
        val denominator = a - 2.0 * b + c
        val delta = if (abs(denominator) < 1e-12) 0.0 else (0.5 * (a - c) / denominator).coerceIn(-1.0, 1.0)
        return (peak + delta) * binHz
    }
}
