package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure procedural audio synthesis for the metronome click generator.
 * Separated from Android AudioTrack APIs for full unit testability.
 */
object MetronomeAudioMath {

    const val DEFAULT_SAMPLE_RATE = 44100
    const val DEFAULT_MAX_AMPLITUDE = 28000.0

    /**
     * Procedurally synthesizes a pleasant woodblock-style click with an exponential decay envelope.
     *
     * @param sampleRate The PCM sample rate (typically 44100 Hz).
     * @param frequencyHz Pitch of the woodblock click in Hertz.
     * @param durationMs Duration of the click audio buffer in milliseconds.
     * @param decay Rate of exponential decay envelope (higher = faster fade).
     * @param maxAmp Peak amplitude before decay (default 28000).
     */
    fun generateClickPcm(
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        frequencyHz: Double,
        durationMs: Int,
        decay: Double,
        maxAmp: Double = DEFAULT_MAX_AMPLITUDE
    ): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-decay * t)
            val sample = sin(2.0 * PI * frequencyHz * t) * envelope * maxAmp
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
