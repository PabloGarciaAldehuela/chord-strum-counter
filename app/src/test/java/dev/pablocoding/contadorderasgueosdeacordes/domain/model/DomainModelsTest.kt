package dev.pablocoding.contadorderasgueosdeacordes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    @Test
    fun `Session default values and copy semantics`() {
        val defaultSession = Session()
        assertEquals(60, defaultSession.durationSeconds)
        assertEquals(0, defaultSession.transitionCount)
        assertFalse(defaultSession.isRunning)
        assertFalse(defaultSession.isFinished)
        assertEquals(60, defaultSession.remainingSeconds)
        assertEquals(listOf("A", "D"), defaultSession.chords)
        assertNull(defaultSession.error)

        val updated = defaultSession.copy(
            transitionCount = 15,
            isRunning = true,
            remainingSeconds = 45,
            chords = listOf("C", "G"),
            error = SessionError.MicrophoneUnavailable
        )
        assertEquals(15, updated.transitionCount)
        assertTrue(updated.isRunning)
        assertEquals(45, updated.remainingSeconds)
        assertEquals(listOf("C", "G"), updated.chords)
        assertEquals(SessionError.MicrophoneUnavailable, updated.error)
    }

    @Test
    fun `SessionResult default values and copy semantics`() {
        val result = SessionResult(
            id = 10L,
            timestamp = 5000L,
            durationSeconds = 60,
            transitionCount = 42
        )
        assertEquals(10L, result.id)
        assertEquals(5000L, result.timestamp)
        assertEquals(60, result.durationSeconds)
        assertEquals(42, result.transitionCount)
        assertEquals(listOf("A", "D"), result.chords)

        val modified = result.copy(chords = listOf("Em", "Am"))
        assertEquals(listOf("Em", "Am"), modified.chords)
    }

    @Test
    fun `Chord model defaults and copy semantics`() {
        val chord = Chord(
            name = "E",
            fullName = "E Major",
            frets = listOf(0, 2, 2, 1, 0, 0)
        )
        assertEquals("E", chord.name)
        assertEquals("E Major", chord.fullName)
        assertEquals(listOf(0, 0, 0, 0, 0, 0), chord.fingers)
        assertEquals(1, chord.baseFret)

        val withFingers = chord.copy(
            fingers = listOf(0, 2, 3, 1, 0, 0),
            baseFret = 1
        )
        assertEquals(listOf(0, 2, 3, 1, 0, 0), withFingers.fingers)
    }

    @Test
    fun `ChordPracticeCount properties`() {
        val count = ChordPracticeCount(chordName = "G", count = 25)
        assertEquals("G", count.chordName)
        assertEquals(25, count.count)
    }
}
