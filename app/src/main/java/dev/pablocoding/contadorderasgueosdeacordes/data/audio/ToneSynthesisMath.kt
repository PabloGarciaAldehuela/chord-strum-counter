package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure synthesis of the tuner's sounds, separated from AudioTrack for unit testability.
 *
 * Uses additive synthesis rather than Karplus-Strong: the pitch is mathematically exact (a
 * Karplus-Strong delay line rounds the period to whole samples, several cents off on high
 * strings) and still sounds plucked thanks to per-harmonic decay.
 */
object ToneSynthesisMath {

    const val DEFAULT_SAMPLE_RATE = 44_100
    private const val PLUCK_POSITION = 0.18
    private const val MAX_HARMONICS = 12
    private const val ATTACK_MS = 4.0
    private const val RELEASE_MS = 80.0

    /**
     * Plucked-string reference tone. Rich in harmonics so low strings remain audible on phone
     * speakers that can't reproduce an 82 Hz fundamental (the ear infers it from the harmonics).
     */
    fun generatePluckedTone(
        frequencyHz: Double,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        durationMs: Int = 1800,
        peak: Double = 0.7
    ): ShortArray {
        val length = sampleRate * durationMs / 1000
        val buffer = DoubleArray(length)
        for (k in 1..MAX_HARMONICS) {
            val f = k * frequencyHz
            if (f >= sampleRate * 0.45) break
            // String plucked at a fraction of its length excites harmonic k by |sin(πkp)|/k.
            val amplitude = abs(sin(PI * k * PLUCK_POSITION)) / k.toDouble().pow(1.1)
            val decay = 1.4 * (1.0 + 0.6 * (k - 1))
            val omega = 2.0 * PI * f / sampleRate
            for (i in 0 until length) {
                buffer[i] += amplitude * exp(-decay * i / sampleRate) * sin(omega * i)
            }
        }
        return normalize(buffer, sampleRate, peak)
    }

    /** Short bright two-partial "ding" played when a string reaches pitch. */
    fun generateChime(sampleRate: Int = DEFAULT_SAMPLE_RATE, durationMs: Int = 380, peak: Double = 0.35): ShortArray {
        val length = sampleRate * durationMs / 1000
        val buffer = DoubleArray(length) { i ->
            val t = i.toDouble() / sampleRate
            0.7 * exp(-9.0 * t) * sin(2.0 * PI * 2093.0 * t) + 0.3 * exp(-14.0 * t) * sin(2.0 * PI * 3136.0 * t)
        }
        return normalize(buffer, sampleRate, peak)
    }

    private fun normalize(buffer: DoubleArray, sampleRate: Int, peak: Double): ShortArray {
        val attack = (ATTACK_MS * sampleRate / 1000).toInt().coerceAtLeast(1)
        val release = (RELEASE_MS * sampleRate / 1000).toInt().coerceAtLeast(1)
        for (i in buffer.indices) {
            val fadeIn = min(1.0, i.toDouble() / attack)
            val fadeOut = min(1.0, (buffer.size - 1 - i).toDouble() / release)
            buffer[i] *= fadeIn * fadeOut
        }
        val max = buffer.maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: return ShortArray(buffer.size)
        val scale = peak * Short.MAX_VALUE / max
        return ShortArray(buffer.size) { (buffer[it] * scale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }
    }
}
