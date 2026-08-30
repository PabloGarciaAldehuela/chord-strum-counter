package dev.pablocoding.contadorderasgueosdeacordes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pablocoding.contadorderasgueosdeacordes.data.db.dao.SessionDao
import dev.pablocoding.contadorderasgueosdeacordes.data.db.entity.SessionResultEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE session_results ADD COLUMN chords TEXT NOT NULL DEFAULT 'A,D'")
    }
}

@Database(
    entities = [SessionResultEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
