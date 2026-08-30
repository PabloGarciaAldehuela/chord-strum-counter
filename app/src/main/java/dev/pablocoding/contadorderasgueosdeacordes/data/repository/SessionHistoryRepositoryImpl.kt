package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import dev.pablocoding.contadorderasgueosdeacordes.data.db.dao.SessionDao
import dev.pablocoding.contadorderasgueosdeacordes.data.db.mapper.toDomain
import dev.pablocoding.contadorderasgueosdeacordes.data.db.mapper.toEntity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionHistoryRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionHistoryRepository {

    override fun getAllSessions(): Flow<List<SessionResult>> =
        sessionDao.getAllSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveSession(result: SessionResult) {
        sessionDao.insertSession(result.toEntity())
    }

    override suspend fun clearAll() {
        sessionDao.clearAll()
    }
}
