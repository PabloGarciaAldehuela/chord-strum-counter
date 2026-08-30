package dev.pablocoding.contadorderasgueosdeacordes.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_results")
data class SessionResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val durationSeconds: Int,
    val transitionCount: Int,
    @ColumnInfo(defaultValue = "A,D")
    val chords: String = "A,D"
)
