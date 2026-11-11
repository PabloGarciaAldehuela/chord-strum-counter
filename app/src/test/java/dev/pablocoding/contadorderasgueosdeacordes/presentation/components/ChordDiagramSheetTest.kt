package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordDiagramSheetTest {

    @Test
    fun `all chords have exactly 6 string fret positions and 6 finger positions`() {
        for (chord in ChordLibrary.allChords) {
            assertEquals("Chord ${chord.name} must have 6 frets", 6, chord.frets.size)
            assertEquals("Chord ${chord.name} must have 6 fingers", 6, chord.fingers.size)
            assertTrue("Chord ${chord.name} baseFret must be positive", chord.baseFret >= 1)
        }
    }

    @Test
    fun `chord list indexing correctly locates initial chord`() {
        val chords = ChordLibrary.allChords
        val target = ChordLibrary.getChord("Cadd9")
        assertNotNull(target)

        val foundIndex = chords.indexOfFirst { it.name.equals(target!!.name, ignoreCase = true) }
        assertTrue("Index of Cadd9 must be valid", foundIndex >= 0)
        assertEquals(target, chords[foundIndex])
    }

    @Test
    fun `fallback to index 0 when initial chord not found in sublist`() {
        val sublist = listOf(
            ChordLibrary.getChord("A")!!,
            ChordLibrary.getChord("D")!!
        )
        val missingChord = ChordLibrary.getChord("Em")!!
        val idx = sublist.indexOfFirst { it.name.equals(missingChord.name, ignoreCase = true) }
        val resolved = if (idx >= 0) idx else 0
        assertEquals(0, resolved)
    }

    @Test
    fun `stuck 3 and 4 chords have fingers on strings 1 and 2`() {
        val stuck34Chords = listOf("Big G", "G5", "Cadd9", "Dsus4", "A7sus4", "A7sus4 (var)", "Em7", "Dadd11/F#", "F6/9")
        for (name in stuck34Chords) {
            val chord = ChordLibrary.getChord(name)
            assertNotNull("Chord $name must exist", chord)
            // String 2 (index 4) and String 1 (index 5) should have fret 3
            assertEquals("Chord $name string 2 fret must be 3", 3, chord!!.frets[4])
            assertEquals("Chord $name string 1 fret must be 3", 3, chord.frets[5])
        }
    }

    @Test
    fun `shouldAbsorbDownwardScroll absorbs downward drift during horizontal swipe`() {
        // Natural thumb swipe with downward arc: 50px horizontal, 30px downward
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardScroll(
            consumedX = 50f,
            availableY = 30f,
            isScrollInProgress = false
        )
        assertTrue("Must absorb downward drift when horizontal motion is present", absorbed)
    }

    @Test
    fun `shouldAbsorbDownwardScroll absorbs downward motion while pager is active`() {
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardScroll(
            consumedX = 0f,
            availableY = 40f,
            isScrollInProgress = true
        )
        assertTrue("Must absorb downward motion while pager scroll is in progress", absorbed)
    }

    @Test
    fun `shouldAbsorbDownwardScroll does not absorb pure downward drag`() {
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardScroll(
            consumedX = 0f,
            availableY = 100f,
            isScrollInProgress = false
        )
        org.junit.Assert.assertFalse("Must allow pure downward drag to reach sheet dismiss handler", absorbed)
    }

    @Test
    fun `shouldAbsorbDownwardScroll ignores upward scroll`() {
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardScroll(
            consumedX = 50f,
            availableY = -20f,
            isScrollInProgress = true
        )
        org.junit.Assert.assertFalse("Must not absorb upward scroll", absorbed)
    }

    @Test
    fun `shouldAbsorbDownwardFling absorbs downward fling velocity on horizontal flick`() {
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardFling(
            velocityX = 1200f,
            velocityY = 300f
        )
        assertTrue("Must absorb downward velocity when horizontal fling dominates", absorbed)
    }

    @Test
    fun `shouldAbsorbDownwardFling does not absorb deliberate vertical flick`() {
        val absorbed = ChordPagerScrollFilter.shouldAbsorbDownwardFling(
            velocityX = 50f,
            velocityY = 1500f
        )
        org.junit.Assert.assertFalse("Must allow vertical downward fling to dismiss sheet", absorbed)
    }

    @Test
    fun `shouldAllowSheetDismiss rejects dismiss while pager is actively scrolling`() {
        org.junit.Assert.assertFalse(ChordPagerScrollFilter.shouldAllowSheetDismiss(isScrollInProgress = true))
        assertTrue(ChordPagerScrollFilter.shouldAllowSheetDismiss(isScrollInProgress = false))
    }
}
