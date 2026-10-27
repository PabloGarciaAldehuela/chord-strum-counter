package dev.pablocoding.contadorderasgueosdeacordes.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerTolerance
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val DURATION_KEY       = intPreferencesKey("practice_duration_seconds")
private val SENSITIVITY_KEY    = floatPreferencesKey("mic_sensitivity")
private val DEBOUNCE_KEY       = intPreferencesKey("strum_debounce_ms")
private val METRONOME_BPM_KEY  = intPreferencesKey("metronome_bpm")
private val CHORDS_KEY         = stringPreferencesKey("selected_practice_chords")

private val TUNER_TUNING_KEY         = stringPreferencesKey("tuner_tuning_id")
private val TUNER_REFERENCE_KEY      = intPreferencesKey("tuner_reference_pitch_hz")
private val TUNER_SOUND_KEY          = booleanPreferencesKey("tuner_in_tune_sound")
private val TUNER_LEFT_HANDED_KEY    = booleanPreferencesKey("tuner_left_handed")
private val TUNER_NOTE_NAMING_KEY    = stringPreferencesKey("tuner_note_naming")
private val TUNER_TOLERANCE_KEY      = stringPreferencesKey("tuner_tolerance")
private val TUNER_READING_MODE_KEY   = stringPreferencesKey("tuner_reading_mode")
private val NOTE_CLOCK_NOTATION_KEY  = stringPreferencesKey("note_clock_notation_mode")

private const val DEFAULT_DURATION      = 60
private const val DEFAULT_SENSITIVITY   = 0.6f
private const val DEFAULT_DEBOUNCE      = 350
private const val DEFAULT_METRONOME_BPM = 80
private const val DEFAULT_CHORDS        = "A,D"

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

    suspend fun getChords(): List<String> =
        dataStore.data.map {
            val raw = it[CHORDS_KEY] ?: DEFAULT_CHORDS
            raw.split(",").map { c -> c.trim() }.filter { c -> c.isNotEmpty() }
        }.first()

    suspend fun saveChords(chords: List<String>) {
        dataStore.edit { it[CHORDS_KEY] = chords.filter { c -> c.isNotBlank() }.joinToString(",") }
    }

    /**
     * @param defaultNoteNaming Used until the user picks a convention (derived from the device locale).
     */
    suspend fun getTunerSettings(defaultNoteNaming: NoteNaming = NoteNaming.LETTERS): TunerSettings =
        dataStore.data.map { prefs ->
            val defaults = TunerSettings()
            TunerSettings(
                tuningId = prefs[TUNER_TUNING_KEY] ?: defaults.tuningId,
                referencePitchHz = (prefs[TUNER_REFERENCE_KEY] ?: defaults.referencePitchHz)
                    .coerceIn(TunerSettings.MIN_REFERENCE_PITCH_HZ, TunerSettings.MAX_REFERENCE_PITCH_HZ),
                inTuneSound = prefs[TUNER_SOUND_KEY] ?: defaults.inTuneSound,
                leftHanded = prefs[TUNER_LEFT_HANDED_KEY] ?: defaults.leftHanded,
                noteNaming = prefs[TUNER_NOTE_NAMING_KEY]
                    ?.let { name -> NoteNaming.entries.firstOrNull { it.name == name } }
                    ?: defaultNoteNaming,
                tolerance = prefs[TUNER_TOLERANCE_KEY]
                    ?.let { name -> TunerTolerance.entries.firstOrNull { it.name == name } }
                    ?: defaults.tolerance,
                readingMode = prefs[TUNER_READING_MODE_KEY]
                    ?.let { name -> TunerReadingMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.readingMode
            )
        }.first()

    suspend fun saveTunerSettings(settings: TunerSettings) {
        dataStore.edit {
            it[TUNER_TUNING_KEY] = settings.tuningId
            it[TUNER_REFERENCE_KEY] = settings.referencePitchHz
            it[TUNER_SOUND_KEY] = settings.inTuneSound
            it[TUNER_LEFT_HANDED_KEY] = settings.leftHanded
            it[TUNER_NOTE_NAMING_KEY] = settings.noteNaming.name
            it[TUNER_TOLERANCE_KEY] = settings.tolerance.name
            it[TUNER_READING_MODE_KEY] = settings.readingMode.name
        }
    }

    suspend fun getNoteClockNotation(): NoteNotationMode =
        dataStore.data.map { prefs ->
            prefs[NOTE_CLOCK_NOTATION_KEY]?.let { name ->
                NoteNotationMode.entries.firstOrNull { it.name == name }
            } ?: NoteNotationMode.DUAL
        }.first()

    suspend fun saveNoteClockNotation(mode: NoteNotationMode) {
        dataStore.edit { it[NOTE_CLOCK_NOTATION_KEY] = mode.name }
    }
}
