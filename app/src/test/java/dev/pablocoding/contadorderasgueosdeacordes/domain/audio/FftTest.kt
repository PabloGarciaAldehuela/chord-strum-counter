package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

class FftTest {

    @Test
    fun `impulse transforms to a flat spectrum`() {
        val fft = Fft(16)
        val re = DoubleArray(16).also { it[0] = 1.0 }
        val im = DoubleArray(16)
        fft.forward(re, im)
        for (k in 0 until 16) {
            assertEquals(1.0, re[k], 1e-12)
            assertEquals(0.0, im[k], 1e-12)
        }
    }

    @Test
    fun `cosine at bin k concentrates energy in bins k and n-k`() {
        val n = 64
        val k = 5
        val fft = Fft(n)
        val re = DoubleArray(n) { cos(2 * PI * k * it / n) }
        val im = DoubleArray(n)
        fft.forward(re, im)
        for (bin in 0 until n) {
            val magnitude = hypot(re[bin], im[bin])
            val expected = if (bin == k || bin == n - k) n / 2.0 else 0.0
            assertEquals("bin $bin", expected, magnitude, 1e-9)
        }
    }

    @Test
    fun `inverse undoes forward`() {
        val n = 256
        val fft = Fft(n)
        val original = DoubleArray(n) { (it * 37 % 11) - 5.0 }
        val re = original.copyOf()
        val im = DoubleArray(n)
        fft.forward(re, im)
        fft.inverse(re, im)
        for (i in 0 until n) {
            assertEquals(original[i], re[i], 1e-9)
            assertEquals(0.0, im[i], 1e-9)
        }
    }

    @Test
    fun `non power of two sizes are rejected`() {
        assertTrue(runCatching { Fft(100) }.isFailure)
    }
}
