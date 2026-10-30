package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Realistic guitar audio for regression tests, modelled on what a phone actually hears
 * (validated against real Pixel 9 Pro XL captures):
 * - stiff-string inharmonicity, bright pluck near the bridge, per-partial decay,
 * - the pluck's sharp attack glide and a broadband pick click,
 * - a phone microphone's bass roll-off,
 * - low-frequency-heavy room noise, optional mains hum,
 * - strings that keep ringing from earlier plucks and sympathetic resonance.
 */
object RealisticGuitarSignals {

    const val SAMPLE_RATE = 48_000

    /** Standard-tuning strings: frequency, inharmonicity B, acoustic loudness when unplugged. */
    data class GuitarString(val frequencyHz: Double, val inharmonicity: Double, val loudness: Double)

    val STANDARD = listOf(
        GuitarString(82.41, 2.5e-5, 1.0), GuitarString(110.0, 2.5e-5, 0.9), GuitarString(146.83, 3e-5, 0.75),
        GuitarString(196.0, 1.0e-4, 0.55), GuitarString(246.94, 4e-5, 0.45), GuitarString(329.63, 1.5e-5, 0.35)
    )

    /** A plucked stiff string (unit-ish amplitude), starting at sample 0. */
    fun pluck(
        frequencyHz: Double,
        inharmonicity: Double,
        seconds: Double,
        glideCents: Double = 8.0,
        decayPerSecond: Double = 0.5,
        seed: Int = 1
    ): DoubleArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        val out = DoubleArray(n)
        val random = Random(seed)
        for (k in 1..30) {
            val stretch = sqrt(1 + inharmonicity * k * k)
            if (k * frequencyHz * stretch > 4000) break
            val amplitude = abs(sin(PI * k * 0.13)) / k
            val decay = decayPerSecond + 0.35 * k
            var phase = random.nextDouble() * 2 * PI
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val glide = 2.0.pow(glideCents * exp(-t / 0.15) / 1200.0)
                phase += 2 * PI * k * frequencyHz * stretch * glide / SAMPLE_RATE
                out[i] += amplitude * exp(-decay * t) * sin(phase)
            }
        }
        // Pick click: 3 ms of broadband noise.
        val click = (0.003 * SAMPLE_RATE).toInt()
        for (i in 0 until minOf(click, n)) out[i] += (random.nextDouble() * 2 - 1) * 1.5 * (1 - i.toDouble() / click)
        return out
    }

    /**
     * Renders a scene: room noise for the whole duration plus events (sample offset, signal, gain).
     * @param noiseDbfs Room noise level; @param humDbfs optional 50 Hz mains hum with harmonics.
     */
    fun scene(
        seconds: Double,
        events: List<Triple<Int, DoubleArray, Double>>,
        noiseDbfs: Double,
        humDbfs: Double? = null,
        startupSilenceMs: Int = 100,
        seed: Int = 3
    ): FloatArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        val mix = DoubleArray(n)
        for ((offset, signal, gain) in events) {
            for (i in signal.indices) if (offset + i < n) mix[offset + i] += signal[i] * gain
        }
        // Phone mic bass roll-off (4th order at 120 Hz).
        val hp1 = Biquad.highPass(SAMPLE_RATE, 120.0)
        val hp2 = Biquad.highPass(SAMPLE_RATE, 120.0)
        val random = Random(seed)
        val noiseAmplitude = 10.0.pow(noiseDbfs / 20)
        var pink = 0.0
        val out = FloatArray(n)
        for (i in 0 until n) {
            pink = 0.97 * pink + 0.03 * (random.nextDouble() * 2 - 1)
            var x = hp2.process(hp1.process(mix[i])) + noiseAmplitude * (0.5 * (random.nextDouble() * 2 - 1) * sqrt(3.0) + 4.0 * pink)
            humDbfs?.let { h ->
                val a = 10.0.pow(h / 20) * sqrt(2.0)
                val t = i.toDouble() / SAMPLE_RATE
                x += a * (sin(2 * PI * 50 * t) + 0.6 * sin(2 * PI * 100 * t) + 0.4 * sin(2 * PI * 150 * t))
            }
            if (i < startupSilenceMs * SAMPLE_RATE / 1000) x = 0.0
            out[i] = x.toFloat()
        }
        return out
    }

    /** Gain that puts a pluck's first 250 ms at [dbfs] RMS. */
    fun gainFor(signal: DoubleArray, dbfs: Double): Double {
        val n = minOf(signal.size, SAMPLE_RATE / 4)
        var sum = 0.0
        for (i in 0 until n) sum += signal[i] * signal[i]
        return 10.0.pow(dbfs / 20) / sqrt(sum / n)
    }
}
