package dev.pablocoding.contadorderasgueosdeacordes.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val DURATION_KEY    = intPreferencesKey("practice_duration_seconds")
private val SENSITIVITY_KEY = floatPreferencesKey("mic_sensitivity")
private val DEBOUNCE_KEY       = intPreferencesKey("strum_debounce_ms")
private val METRONOME_BPM_KEY  = intPreferencesKey("metronome_bpm")

private const val DEFAULT_DURATION      = 60
private const val DEFAULT_SENSITIVITY   = 0.6f
private const val DEFAULT_DEBOUNCE      = 350
private const val DEFAULT_METRONOME_BPM = 80

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getDuration(): Int =
        dataStore.data.map { it[DURATION_KEY] ?: DEFAULT_DURATION }.first()

    suspend fun saveDuration(seconds: Int) {
        dataStore.edit { it[DURATION_KEY] = seconds }
    }

    suspend fun getSensitivity(): Float =
        dataStore.data.map { it[SENSITIVITY_KEY] ?: DEFAULT_SENSITIVITY }.first()

    suspend fun saveSensitivity(value: Float) {
        dataStore.edit { it[SENSITIVITY_KEY] = value }
    }

    suspend fun getDebounce(): Int =
        dataStore.data.map { it[DEBOUNCE_KEY] ?: DEFAULT_DEBOUNCE }.first()

    suspend fun saveDebounce(ms: Int) {
        dataStore.edit { it[DEBOUNCE_KEY] = ms }
    }

    suspend fun getMetronomeBpm(): Int =
        dataStore.data.map { it[METRONOME_BPM_KEY] ?: DEFAULT_METRONOME_BPM }.first()

    suspend fun saveMetronomeBpm(bpm: Int) {
        dataStore.edit { it[METRONOME_BPM_KEY] = bpm }
    }
}
