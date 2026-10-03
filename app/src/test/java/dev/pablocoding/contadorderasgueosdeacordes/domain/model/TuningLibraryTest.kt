package dev.pablocoding.contadorderasgueosdeacordes.domain.model

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningCategory
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningLibraryTest {

    // Published equal-temperament frequencies at A4 = 440 Hz, string 6 → 1.
    private val expectedFrequencies = mapOf(
        "standard" to listOf(82.41, 110.00, 146.83, 196.00, 246.94, 329.63),
        "drop_d" to listOf(73.42, 110.00, 146.83, 196.00, 246.94, 329.63),
        "drop_c_sharp" to listOf(69.30, 103.83, 138.59, 185.00, 233.08, 311.13),
        "drop_c" to listOf(65.41, 98.00, 130.81, 174.61, 220.00, 293.66),
        "drop_b" to listOf(61.74, 92.50, 123.47, 164.81, 207.65, 277.18),
        "drop_a" to listOf(55.00, 82.41, 110.00, 146.83, 185.00, 246.94),
        "half_step_down" to listOf(77.78, 103.83, 138.59, 185.00, 233.08, 311.13),
        "full_step_down" to listOf(73.42, 98.00, 130.81, 174.61, 220.00, 293.66),
        "open_d" to listOf(73.42, 110.00, 146.83, 185.00, 220.00, 293.66),
        "open_g" to listOf(73.42, 98.00, 146.83, 196.00, 246.94, 293.66),
        "open_e" to listOf(82.41, 123.47, 164.81, 207.65, 246.94, 329.63),
        "open_a" to listOf(82.41, 110.00, 164.81, 220.00, 277.18, 329.63),
        "open_c" to listOf(65.41, 98.00, 130.81, 196.00, 261.63, 329.63),
        "dadgad" to listOf(73.42, 110.00, 146.83, 196.00, 220.00, 293.66)
    )

    @Test
    fun `every tuning matches published frequencies within 0_01 Hz`() {
        for ((id, frequencies) in expectedFrequencies) {
            val tuning = TuningLibrary.getTuning(id)
            assertEquals("Unknown tuning $id", id, tuning.id)
            val actual = tuning.strings.map { PitchMath.midiToFrequency(it) }
            for (i in frequencies.indices) {
                assertEquals("$id string index $i", frequencies[i], actual[i], 0.01)
            }
        }
    }

    @Test
    fun `all guitar tunings have six strings and the lowest string first`() {
        TuningLibrary.allTunings.filterNot { it.isChromatic }.forEach { tuning ->
            assertEquals(tuning.id, 6, tuning.strings.size)
            assertEquals(tuning.id, tuning.strings.min(), tuning.strings.first())
        }
    }

    @Test
    fun `ids are unique and every listed tuning has expected frequencies`() {
        val ids = TuningLibrary.allTunings.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ids.filterNot { it == TuningLibrary.CHROMATIC_ID }.toSet(), expectedFrequencies.keys)
    }

    @Test
    fun `standard is the default and unknown ids fall back to it`() {
        assertEquals(TuningLibrary.standard, TuningLibrary.getTuning("removed_tuning"))
        assertEquals(TuningCategory.STANDARD, TuningLibrary.standard.category)
    }

    @Test
    fun `chromatic tuning has no strings`() {
        val chromatic = TuningLibrary.getTuning(TuningLibrary.CHROMATIC_ID)
        assertTrue(chromatic.isChromatic)
        assertEquals(TuningCategory.CHROMATIC, chromatic.category)
    }

    @Test
    fun `flat tunings are spelled with flats`() {
        val halfStep = TuningLibrary.getTuning("half_step_down")
        assertTrue(halfStep.preferFlats)
        assertEquals("E♭", PitchMath.noteName(halfStep.strings.first(), halfStep.preferFlats))
    }

    @Test
    fun `string numbers count down from the lowest string`() {
        assertEquals(6, TuningLibrary.standard.stringNumber(0))
        assertEquals(1, TuningLibrary.standard.stringNumber(5))
    }
}
