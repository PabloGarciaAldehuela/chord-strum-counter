package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Second-order IIR filter (RBJ Audio EQ Cookbook, Butterworth Q = 1/√2), stateful across calls so
 * it can filter a continuous audio stream chunk by chunk.
 */
class Biquad private constructor(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }

    companion object {
        private const val BUTTERWORTH_Q = 0.7071067811865476

        fun lowPass(sampleRate: Int, cutoffHz: Double): Biquad {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val alpha = sin(w0) / (2.0 * BUTTERWORTH_Q)
            val c = cos(w0)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = (1.0 - c) / 2.0 / a0,
                b1 = (1.0 - c) / a0,
                b2 = (1.0 - c) / 2.0 / a0,
                a1 = -2.0 * c / a0,
                a2 = (1.0 - alpha) / a0
            )
        }

        /** Narrow band-stop at [centerHz]; bandwidth ≈ centerHz / [q]. */
        fun notch(sampleRate: Int, centerHz: Double, q: Double): Biquad {
            val w0 = 2.0 * PI * centerHz / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            val c = cos(w0)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = 1.0 / a0,
                b1 = -2.0 * c / a0,
                b2 = 1.0 / a0,
                a1 = -2.0 * c / a0,
                a2 = (1.0 - alpha) / a0
            )
        }

        fun highPass(sampleRate: Int, cutoffHz: Double): Biquad {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val alpha = sin(w0) / (2.0 * BUTTERWORTH_Q)
            val c = cos(w0)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = (1.0 + c) / 2.0 / a0,
                b1 = -(1.0 + c) / a0,
                b2 = (1.0 + c) / 2.0 / a0,
                a1 = -2.0 * c / a0,
                a2 = (1.0 - alpha) / a0
            )
        }
    }
}
