package dev.pablocoding.contadorderasgueosdeacordes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.pablocoding.contadorderasgueosdeacordes.data.db.dao.SessionDao
import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity

@Database(
    entities = [SessionResultEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
