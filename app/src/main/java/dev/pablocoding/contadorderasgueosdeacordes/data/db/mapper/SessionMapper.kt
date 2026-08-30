package dev.pablocoding.contadorderasgueosdeacordes.data.db.mapper

import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult

fun SessionResultEntity.toDomain(): SessionResult = SessionResult(
    id = id,
    timestamp = timestamp,
    durationSeconds = durationSeconds,
    transitionCount = transitionCount,
    chords = parseChords(chords)
)

fun SessionResult.toEntity(): SessionResultEntity = SessionResultEntity(
    id = id,
    timestamp = timestamp,
    durationSeconds = durationSeconds,
    transitionCount = transitionCount,
    chords = formatChords(chords)
)

private fun parseChords(chordsStr: String): List<String> {
    if (chordsStr.isBlank()) return listOf("A", "D")
    return chordsStr.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf("A", "D") }
}

private fun formatChords(chords: List<String>): String {
    return chords.filter { it.isNotBlank() }.joinToString(",")
}
