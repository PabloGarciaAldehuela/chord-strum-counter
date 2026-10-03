package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.MpmPitchDetector
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthesisMathTest {

    private val sampleRate = ToneSynthesisMath.DEFAULT_SAMPLE_RATE

    private fun ShortArray.window(startSeconds: Double, size: Int): FloatArray {
        val start = (startSeconds * sampleRate).toInt()
        return FloatArray(size) { this[start + it] / Short.MAX_VALUE.toFloat() }
    }

    @Test
    fun `reference tones are exactly in pitch according to our own detector`() {
        val detector = MpmPitchDetector(sampleRate, 4096)
        val notes = (TuningLibrary.allTunings.flatMap { it.strings }).distinct()
        for (midi in notes) {
            val f = PitchMath.midiToFrequency(midi)
            val tone = ToneSynthesisMath.generatePluckedTone(f, sampleRate)
            val estimate = detector.detect(tone.window(0.3, 4096))!!
            val error = PitchMath.cents(estimate.frequencyHz, f)
            assertTrue("Reference tone for MIDI $midi off by $error cents", abs(error) <= 0.5)
        }
    }

    @Test
    fun `reference tone respects duration and peak level`() {
        val tone = ToneSynthesisMath.generatePluckedTone(110.0, sampleRate, durationMs = 1000, peak = 0.5)
        assertEquals(sampleRate, tone.size)
        val peak = tone.maxOf { abs(it.toInt()) }
        assertTrue(peak <= Short.MAX_VALUE * 0.5 + 1)
        assertTrue(peak > Short.MAX_VALUE * 0.45)
    }

    @Test
    fun `sounds fade in and out without clicks`() {
        for (sound in listOf(ToneSynthesisMath.generatePluckedTone(82.41, sampleRate), ToneSynthesisMath.generateChime(sampleRate))) {
            assertEquals(0, sound.first().toInt())
            assertTrue(abs(sound.last().toInt()) < 50)
        }
    }

    @Test
    fun `chime sits above the tuner's detection range`() {
        // The chime partials are 2093 Hz and 3136 Hz, above the detector's 1400 Hz ceiling; the
        // pipeline additionally mutes detection while it plays.
        val chime = ToneSynthesisMath.generateChime(sampleRate)
        assertEquals(sampleRate * 380 / 1000, chime.size)
    }
}
