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
    }

    @Test
    fun `getChord returns chord when found case-insensitively`() {
        val chord = useCase.getChord("am")
        assertNotNull(chord)
        assertEquals("Am", chord?.name)
        assertEquals("A Minor", chord?.fullName)
    }

    @Test
    fun `getChord returns null when chord name does not exist`() {
        val chord = useCase.getChord("Hmaj7_invalid")
        assertNull(chord)
    }
}
