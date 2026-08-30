package dev.pablocoding.contadorderasgueosdeacordes.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pablocoding.contadorderasgueosdeacordes.data.db.AppDatabase
import dev.pablocoding.contadorderasgueosdeacordes.data.db.dao.SessionDao
import dev.pablocoding.contadorderasgueosdeacordes.data.repository.MetronomeRepositoryImpl
import dev.pablocoding.contadorderasgueosdeacordes.data.repository.SessionHistoryRepositoryImpl
import dev.pablocoding.contadorderasgueosdeacordes.data.repository.SessionRepositoryImpl
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chord_counter_prefs")

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSessionHistoryRepository(impl: SessionHistoryRepositoryImpl): SessionHistoryRepository

    @Binds
    @Singleton
    abstract fun bindMetronomeRepository(impl: MetronomeRepositoryImpl): MetronomeRepository

    companion object {

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.dataStore

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "chord_counter_db"
            ).build()

        @Provides
        @Singleton
        fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    }
}
