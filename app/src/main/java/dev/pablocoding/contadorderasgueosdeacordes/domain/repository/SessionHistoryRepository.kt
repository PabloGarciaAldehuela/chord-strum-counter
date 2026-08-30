package dev.pablocoding.contadorderasgueosdeacordes.domain.repository

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    fun getAllSessions(): Flow<List<SessionResult>>
    suspend fun saveSession(result: SessionResult)
    suspend fun clearAll()
}
