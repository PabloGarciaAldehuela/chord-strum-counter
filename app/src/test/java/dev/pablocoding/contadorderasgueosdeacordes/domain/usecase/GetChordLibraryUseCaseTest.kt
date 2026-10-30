package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetChordLibraryUseCaseTest {

    private lateinit var useCase: GetChordLibraryUseCase

    @Before
    fun setUp() {
        useCase = GetChordLibraryUseCase()
    }

    @Test
    fun `invoke returns non-empty list of guitar chords`() {
        val chords = useCase()
        assertTrue(chords.isNotEmpty())
        assertTrue(chords.any { it.name == "A" })
        assertTrue(chords.any { it.name == "D" })
        assertTrue(chords.any { it.name == "Em" })
        assertTrue(chords.any { it.name == "Big G" })
        assertTrue(chords.any { it.name == "Cadd9" })
        assertTrue(chords.any { it.name == "Dadd11/F#" })
    }

    @Test
    fun `getChord returns chord when found case-insensitively`() {
        val chord = useCase.getChord("am")
        assertNotNull(chord)
        assertEquals("Am", chord?.name)
        assertEquals("A Minor", chord?.fullName)

        val cadd9 = useCase.getChord("cadd9")
        assertNotNull(cadd9)
        assertEquals("Cadd9", cadd9?.name)
        assertEquals("C Added 9th", cadd9?.fullName)
    }

    @Test
    fun `getChord returns null when chord name does not exist`() {
        val chord = useCase.getChord("Hmaj7_invalid")
        assertNull(chord)
    }
}
