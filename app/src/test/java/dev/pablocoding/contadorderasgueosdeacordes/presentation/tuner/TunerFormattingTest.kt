package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerFormattingTest {

    @Test
    fun `direction tells the user which way to turn the peg`() {
        assertEquals(TuneDirection.TUNE_UP, TunerFormatting.direction(-8.0, isInTune = false))
        assertEquals(TuneDirection.TUNE_DOWN, TunerFormatting.direction(8.0, isInTune = false))
        assertEquals(TuneDirection.TUNE_UP_A_LOT, TunerFormatting.direction(-60.0, isInTune = false))
        assertEquals(TuneDirection.TUNE_DOWN_A_LOT, TunerFormatting.direction(200.0, isInTune = false))
        assertEquals(TuneDirection.IN_TUNE, TunerFormatting.direction(4.0, isInTune = true))
    }

    @Test
    fun `cents labels are signed and clamp far deviations`() {
        assertEquals("+7", TunerFormatting.centsLabel(7.2))
        assertEquals("−12", TunerFormatting.centsLabel(-11.6))
        assertEquals("0", TunerFormatting.centsLabel(-0.4))
        assertEquals("≪", TunerFormatting.centsLabel(-150.0))
        assertEquals("≫", TunerFormatting.centsLabel(99.6))
    }

    @Test
    fun `deviation levels follow the tolerance`() {
        assertEquals(DeviationLevel.IN_TUNE, TunerFormatting.deviationLevel(-3.0, 3.0))
        assertEquals(DeviationLevel.CLOSE, TunerFormatting.deviationLevel(3.5, 3.0))
        assertEquals(DeviationLevel.CLOSE, TunerFormatting.deviationLevel(-15.0, 3.0))
        assertEquals(DeviationLevel.FAR, TunerFormatting.deviationLevel(16.0, 3.0))
    }

    @Test
    fun `display position is centred, monotonic, magnified near zero and clamped`() {
        assertEquals(0f, TunerFormatting.displayPosition(0.0), 0f)
        assertEquals(1f, TunerFormatting.displayPosition(50.0), 1e-6f)
        assertEquals(-1f, TunerFormatting.displayPosition(-500.0), 1e-6f)
        var previous = -1.1f
        for (c in -50..50) {
            val position = TunerFormatting.displayPosition(c.toDouble())
            assertTrue(position > previous)
            previous = position
        }
        assertTrue("5¢ should be visibly off-centre", TunerFormatting.displayPosition(5.0) > 0.15f)
    }

    @Test
    fun `frequency label uses one decimal regardless of locale`() {
        assertEquals("82.4 Hz", TunerFormatting.frequencyLabel(82.4069))
    }

    @Test
    fun `accessibility description reads naturally`() {
        val flat = TunerReading(frequencyHz = 81.9, detectedMidi = 40, targetMidi = 40, targetStringIndex = 0, cents = -5.2)
        assertEquals("E 2, 5 cents flat, tune up", TunerFormatting.accessibilityDescription(flat, TuningLibrary.standard, NoteNaming.LETTERS))
        val inTune = flat.copy(cents = 0.5, isInTune = true)
        assertEquals("Mi 2, in tune", TunerFormatting.accessibilityDescription(inTune, TuningLibrary.standard, NoteNaming.SOLFEGE))
        assertEquals("Listening. Pluck a string.", TunerFormatting.accessibilityDescription(TunerReading(), TuningLibrary.standard, NoteNaming.LETTERS))
    }

    @Test
    fun `tuning summary lists notes low to high`() {
        assertEquals("E A D G B E", tuningNotesSummary(TuningLibrary.standard, NoteNaming.LETTERS))
        assertEquals("E♭ A♭ D♭ G♭ B♭ E♭", tuningNotesSummary(TuningLibrary.getTuning("half_step_down"), NoteNaming.LETTERS))
        assertEquals("Any note", tuningNotesSummary(TuningLibrary.chromatic, NoteNaming.LETTERS))
    }

    @Test
    fun `level meter maps -80 to -30 dBFS onto 0 to 1`() {
        assertEquals(0f, TunerFormatting.levelFraction(0.0), 0f)
        assertEquals(0f, TunerFormatting.levelFraction(0.00001), 1e-6f)      // -100 dBFS
        assertEquals(0.4f, TunerFormatting.levelFraction(0.001), 1e-3f)      // -60 dBFS
        assertEquals(1f, TunerFormatting.levelFraction(0.5), 0f)
    }
}
