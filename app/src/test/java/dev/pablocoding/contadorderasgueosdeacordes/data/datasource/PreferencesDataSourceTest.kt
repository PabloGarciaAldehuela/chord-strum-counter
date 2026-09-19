package dev.pablocoding.contadorderasgueosdeacordes.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var preferencesDataSource: PreferencesDataSource

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_preferences.preferences_pb") }
        )
        preferencesDataSource = PreferencesDataSource(testDataStore)
    }

    @Test
    fun `returns default duration when not set`() = runTest(testDispatcher) {
        assertEquals(60, preferencesDataSource.getDuration())
    }

    @Test
    fun `saves and retrieves custom duration`() = runTest(testDispatcher) {
        preferencesDataSource.saveDuration(90)
        assertEquals(90, preferencesDataSource.getDuration())
    }

    @Test
    fun `returns default sensitivity when not set`() = runTest(testDispatcher) {
        assertEquals(0.6f, preferencesDataSource.getSensitivity(), 0.001f)
    }

    @Test
    fun `saves and retrieves custom sensitivity`() = runTest(testDispatcher) {
        preferencesDataSource.saveSensitivity(0.85f)
        assertEquals(0.85f, preferencesDataSource.getSensitivity(), 0.001f)
    }

    @Test
    fun `returns default debounce when not set`() = runTest(testDispatcher) {
        assertEquals(350, preferencesDataSource.getDebounce())
    }

    @Test
    fun `saves and retrieves custom debounce`() = runTest(testDispatcher) {
        preferencesDataSource.saveDebounce(500)
        assertEquals(500, preferencesDataSource.getDebounce())
    }

    @Test
    fun `returns default metronome BPM when not set`() = runTest(testDispatcher) {
        assertEquals(80, preferencesDataSource.getMetronomeBpm())
    }

    @Test
    fun `saves and retrieves custom metronome BPM`() = runTest(testDispatcher) {
        preferencesDataSource.saveMetronomeBpm(120)
        assertEquals(120, preferencesDataSource.getMetronomeBpm())
    }

    @Test
    fun `returns default chords when not set`() = runTest(testDispatcher) {
        assertEquals(listOf("A", "D"), preferencesDataSource.getChords())
    }

    @Test
    fun `saves and retrieves custom chords filtering empty and whitespace`() = runTest(testDispatcher) {
        preferencesDataSource.saveChords(listOf("C", "  ", "G", "", "Am"))
        assertEquals(listOf("C", "G", "Am"), preferencesDataSource.getChords())
    }
}
