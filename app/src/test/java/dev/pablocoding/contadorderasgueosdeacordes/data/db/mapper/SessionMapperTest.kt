package dev.pablocoding.contadorderasgueosdeacordes.data.db.mapper

import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionMapperTest {

    @Test
    fun `SessionResultEntity toDomain maps all fields including chords correctly`() {
        val entity = SessionResultEntity(
            id = 42L,
            timestamp = 1700000000000L,
            durationSeconds = 60,
            transitionCount = 55,
            chords = "A,D,E"
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(1700000000000L, domain.timestamp)
        assertEquals(60, domain.durationSeconds)
        assertEquals(55, domain.transitionCount)
        assertEquals(listOf("A", "D", "E"), domain.chords)
    }

    @Test
    fun `SessionResult toEntity maps all fields including chords correctly`() {
        val domain = SessionResult(
            id = 100L,
            timestamp = 1700000050000L,
            durationSeconds = 120,
            transitionCount = 88,
            chords = listOf("C", "G", "Am", "F")
        )

        val entity = domain.toEntity()

        assertEquals(100L, entity.id)
        assertEquals(1700000050000L, entity.timestamp)
        assertEquals(120, entity.durationSeconds)
        assertEquals(88, entity.transitionCount)
        assertEquals("C,G,Am,F", entity.chords)
    }

    @Test
    fun `SessionResultEntity toDomain defaults to A and D when chords string is blank or commas only`() {
        val blankEntity = SessionResultEntity(
            id = 1L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = ""
        )
        assertEquals(listOf("A", "D"), blankEntity.toDomain().chords)

        val whitespaceEntity = SessionResultEntity(
            id = 2L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = "   "
        )
        assertEquals(listOf("A", "D"), whitespaceEntity.toDomain().chords)

        val commasEntity = SessionResultEntity(
            id = 3L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = " , , "
        )
        assertEquals(listOf("A", "D"), commasEntity.toDomain().chords)
    }

    @Test
    fun `SessionResultEntity toDomain trims whitespace from chords`() {
        val messyEntity = SessionResultEntity(
            id = 1L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = "  A ,  D  , Em  "
        )
        assertEquals(listOf("A", "D", "Em"), messyEntity.toDomain().chords)
    }

    @Test
    fun `SessionResult toEntity filters out blank entries and handles empty list`() {
        val emptyChordsResult = SessionResult(
            id = 1L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = emptyList()
        )
        assertEquals("", emptyChordsResult.toEntity().chords)

        val blanksResult = SessionResult(
            id = 2L,
            timestamp = 1000L,
            durationSeconds = 60,
            transitionCount = 10,
            chords = listOf("A", " ", "", "D")
        )
        assertEquals("A,D", blanksResult.toEntity().chords)
    }
}
