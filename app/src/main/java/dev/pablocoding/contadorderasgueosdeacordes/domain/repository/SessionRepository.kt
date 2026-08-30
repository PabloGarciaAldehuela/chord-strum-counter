package dev.pablocoding.contadorderasgueosdeacordes.domain.repository

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessionFlow: Flow<Session>
    suspend fun startSession(durationSeconds: Int, chords: List<String> = listOf("A", "D"))
    suspend fun stopSession()
    suspend fun registerTransition()
    suspend fun getPreferredDuration(): Int
    suspend fun savePreferredDuration(seconds: Int)
    suspend fun getPreferredSensitivity(): Float
    suspend fun savePreferredSensitivity(value: Float)
    suspend fun getPreferredDebounce(): Int
    suspend fun savePreferredDebounce(ms: Int)
    suspend fun getPreferredChords(): List<String>
    suspend fun savePreferredChords(chords: List<String>)
}
