package dev.pablocoding.contadorderasgueosdeacordes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MetronomeStateTest {

    @Test
    fun `default values are set correctly`() {
        val state = MetronomeState()
        assertEquals(false, state.isPlaying)
        assertEquals(80, state.bpm)
        assertEquals(4, state.beatsPerMeasure)
        assertEquals(1, state.currentBeat)
        assertEquals("Andante", state.tempoName)
    }

    @Test
    fun `tempoName correctly categorizes different BPM ranges`() {
        assertEquals("Largo", MetronomeState(bpm = 40).tempoName)
        assertEquals("Largo", MetronomeState(bpm = 59).tempoName)
        assertEquals("Larghetto", MetronomeState(bpm = 60).tempoName)
        assertEquals("Larghetto", MetronomeState(bpm = 65).tempoName)
        assertEquals("Adagio", MetronomeState(bpm = 66).tempoName)
        assertEquals("Adagio", MetronomeState(bpm = 75).tempoName)
        assertEquals("Andante", MetronomeState(bpm = 76).tempoName)
        assertEquals("Andante", MetronomeState(bpm = 107).tempoName)
        assertEquals("Moderato", MetronomeState(bpm = 108).tempoName)
        assertEquals("Moderato", MetronomeState(bpm = 119).tempoName)
        assertEquals("Allegro", MetronomeState(bpm = 120).tempoName)
        assertEquals("Allegro", MetronomeState(bpm = 167).tempoName)
        assertEquals("Presto", MetronomeState(bpm = 168).tempoName)
        assertEquals("Presto", MetronomeState(bpm = 199).tempoName)
        assertEquals("Prestissimo", MetronomeState(bpm = 200).tempoName)
        assertEquals("Prestissimo", MetronomeState(bpm = 240).tempoName)
    }
}
