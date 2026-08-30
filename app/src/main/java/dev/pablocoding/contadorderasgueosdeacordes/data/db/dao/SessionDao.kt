package dev.pablocoding.contadorderasgueosdeacordes.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionResultEntity)

    @Query("SELECT * FROM session_results ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionResultEntity>>

    @Query("DELETE FROM session_results")
    suspend fun clearAll()
}
