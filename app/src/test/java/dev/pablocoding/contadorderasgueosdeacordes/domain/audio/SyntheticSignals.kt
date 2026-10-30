package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Deterministic synthetic audio used by the tuner accuracy tests. */
object SyntheticSignals {

    fun sine(frequencyHz: Double, sampleRate: Int, length: Int, amplitude: Double = 0.5, phase: Double = 0.3): FloatArray =
        FloatArray(length) { i -> (amplitude * sin(2.0 * PI * frequencyHz * i / sampleRate + phase)).toFloat() }

    /**
     * Guitar-like tone: harmonics 1..[harmonics] with 1/k amplitudes, the fundamental attenuated by
     * [fundamentalGainDb] (phone microphones roll off low frequencies), optional exponential decay.
     */
    fun string(
        frequencyHz: Double,
        sampleRate: Int,
        length: Int,
        fundamentalGainDb: Double = 0.0,
        harmonics: Int = 8,
        decayPerSecond: Double = 0.0,
        amplitude: Double = 0.4,
        startOffset: Int = 0
    ): FloatArray {
        val fundamentalGain = Math.pow(10.0, fundamentalGainDb / 20.0)
        val raw = DoubleArray(length) { i ->
            val t = (i + startOffset).toDouble() / sampleRate
            var sum = 0.0
            for (k in 1..harmonics) {
                val f = k * frequencyHz
                if (f >= sampleRate / 2.0) break
                val gain = if (k == 1) fundamentalGain else 1.0 / k
                sum += gain * sin(2.0 * PI * f * t + 0.7 * k)
            }
            sum * exp(-decayPerSecond * t)
        }
        val peak = raw.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        return FloatArray(length) { (raw[it] / peak * amplitude).toFloat() }
    }

    /** Adds white noise at the given signal-to-noise ratio (in dB). */
    fun withNoise(signal: FloatArray, snrDb: Double, seed: Int = 7): FloatArray {
        val random = Random(seed)
        val signalRms = rms(signal)
        val noiseRms = signalRms / Math.pow(10.0, snrDb / 20.0)
        // Uniform noise in [-a, a] has RMS a / sqrt(3).
        val a = noiseRms * sqrt(3.0)
        return FloatArray(signal.size) { (signal[it] + (random.nextDouble() * 2 - 1) * a).toFloat() }
    }

    fun noise(length: Int, amplitude: Double, seed: Int = 11): FloatArray {
        val random = Random(seed)
        return FloatArray(length) { ((random.nextDouble() * 2 - 1) * amplitude).toFloat() }
    }

    fun rms(signal: FloatArray): Double = sqrt(signal.sumOf { it.toDouble() * it } / signal.size)

    fun cents(measured: Double, expected: Double): Double = 1200.0 * ln(measured / expected) / ln(2.0)

    fun withCents(frequencyHz: Double, cents: Double): Double = frequencyHz * Math.pow(2.0, cents / 1200.0)
}
