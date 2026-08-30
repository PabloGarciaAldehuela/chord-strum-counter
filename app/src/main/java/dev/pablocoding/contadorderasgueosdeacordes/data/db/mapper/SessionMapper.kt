package dev.pablocoding.contadorderasgueosdeacordes.data.db.mapper

import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult

fun SessionResultEntity.toDomain(): SessionResult = SessionResult(
    id = id,
    timestamp = timestamp,
    durationSeconds = durationSeconds,
    transitionCount = transitionCount
)

fun SessionResult.toEntity(): SessionResultEntity = SessionResultEntity(
    id = id,
    timestamp = timestamp,
    durationSeconds = durationSeconds,
    transitionCount = transitionCount
)
