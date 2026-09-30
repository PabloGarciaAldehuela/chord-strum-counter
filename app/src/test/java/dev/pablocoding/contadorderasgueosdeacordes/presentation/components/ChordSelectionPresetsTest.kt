package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordSelectionPresetsTest {

    @Test
    fun `defaultPresets contains expected presets count`() {
        val presets = ChordSelectionPresets.defaultPresets
        assertEquals(12, presets.size)
    }

    @Test
    fun `each preset label matches its chord sequence joined by bidirectional arrow`() {
        for ((label, chords) in ChordSelectionPresets.defaultPresets) {
            val expectedLabel = chords.joinToString(" ⇄ ")
            assertEquals("Label must match chord sequence joined by arrow", expectedLabel, label)
            assertTrue("Preset must contain between 2 and 6 chords", chords.size in 2..6)
        }
    }

    @Test
    fun `all chords in defaultPresets exist in ChordLibrary`() {
        for ((_, chords) in ChordSelectionPresets.defaultPresets) {
            for (chordName in chords) {
                val chord = ChordLibrary.getChord(chordName)
                assertNotNull("Chord '$chordName' in presets must exist in ChordLibrary", chord)
            }
        }
    }

    @Test
    fun `all requested quick presets are present in defaultPresets`() {
        val expectedPresets = listOf(
            listOf("Am", "Dm"),
            listOf("A", "D"),
            listOf("A", "E"),
            listOf("D", "E"),
            listOf("Am", "E"),
            listOf("Em", "D"),
            listOf("Am", "Em"),
            listOf("Am", "C"),
            listOf("C", "Em")
        )

        val actualChordsList = ChordSelectionPresets.defaultPresets.map { it.second }

        for (expected in expectedPresets) {
            assertTrue(
                "Preset progression $expected must be included in defaultPresets",
                actualChordsList.contains(expected)
            )
        }
    }

    @Test
    fun `all defaultPresets have unique chord progressions`() {
        val chordProgressions = ChordSelectionPresets.defaultPresets.map { it.second }
        val uniqueProgressions = chordProgressions.toSet()
        assertEquals(
            "Every preset progression must be unique",
            uniqueProgressions.size,
            chordProgressions.size
        )
    }
}
