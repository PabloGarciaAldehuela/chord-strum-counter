package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MetronomeAudioMathTest {

    @Test
    fun `generateClickPcm produces exact number of samples for duration`() {
        val sampleRate = 44100
        val durationMs = 25
        val expectedSamples = (44100 * (25 / 1000.0)).toInt() // 1102 samples

        val samples = MetronomeAudioMath.generateClickPcm(
            sampleRate = sampleRate,
            frequencyHz = 1600.0,
            durationMs = durationMs,
            decay = 80.0
        )

        assertEquals(expectedSamples, samples.size)
    }

    @Test
    fun `generateClickPcm stays within 16-bit short boundaries without clipping`() {
        val samples = MetronomeAudioMath.generateClickPcm(
            sampleRate = 44100,
            frequencyHz = 1600.0,
            durationMs = 50,
            decay = 50.0,
            maxAmp = 28000.0
        )

        for (sample in samples) {
            assertTrue("Sample $sample out of short bounds", sample in Short.MIN_VALUE..Short.MAX_VALUE)
            assertTrue("Sample $sample exceeded configured max amplitude", abs(sample.toInt()) <= 28000)
        }
    }

    @Test
    fun `generateClickPcm exhibits exponential decay towards zero`() {
        val samples = MetronomeAudioMath.generateClickPcm(
            sampleRate = 44100,
            frequencyHz = 1000.0,
            durationMs = 100,
            decay = 100.0,
            maxAmp = 28000.0
        )

        // Split into first quarter and last quarter
        val quarterSize = samples.size / 4
        var earlyMax = 0
        for (i in 0 until quarterSize) {
            earlyMax = maxOf(earlyMax, abs(samples[i].toInt()))
        }

        var lateMax = 0
        for (i in (samples.size - quarterSize) until samples.size) {
            lateMax = maxOf(lateMax, abs(samples[i].toInt()))
        }

        assertTrue("Early waveform peak ($earlyMax) should be significant", earlyMax > 15000)
        assertTrue("Late waveform peak ($lateMax) should be decayed far below early peak ($earlyMax)", lateMax < earlyMax / 10)
    }

    @Test
    fun `generateClickPcm handles 0 durationMs gracefully`() {
        val samples = MetronomeAudioMath.generateClickPcm(
            sampleRate = 44100,
            frequencyHz = 1000.0,
            durationMs = 0,
            decay = 100.0
        )
        assertEquals(0, samples.size)
    }
}
