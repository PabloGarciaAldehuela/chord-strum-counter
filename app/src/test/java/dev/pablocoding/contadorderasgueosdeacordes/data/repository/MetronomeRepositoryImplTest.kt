package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import app.cash.turbine.test
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.MetronomeEngine
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeRepositoryImplTest {

    private val metronomeEngine: MetronomeEngine = mockk(relaxed = true)
    private val preferencesDataSource: PreferencesDataSource = mockk(relaxed = true)

    private val isPlayingFlow = MutableStateFlow(false)
    private val currentBeatFlow = MutableStateFlow(1)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        every { metronomeEngine.isPlaying } returns isPlayingFlow
        every { metronomeEngine.currentBeat } returns currentBeatFlow
        coEvery { preferencesDataSource.getMetronomeBpm() } returns 100
    }

    private fun createRepository(): MetronomeRepositoryImpl {
        return MetronomeRepositoryImpl(
            metronomeEngine = metronomeEngine,
            preferencesDataSource = preferencesDataSource,
            appScope = testScope
        )
    }

    @Test
    fun `initialization loads BPM from preferences and updates metronomeState`() = runTest(testDispatcher) {
        val repository = createRepository()
        advanceUntilIdle()

        repository.metronomeState.test {
            val state = awaitItem()
            assertEquals(100, state.bpm)
            assertEquals(false, state.isPlaying)
            assertEquals(1, state.currentBeat)
            assertEquals(4, state.beatsPerMeasure)
        }
    }

    @Test
    fun `start delegates to metronomeEngine with current BPM`() = runTest(testDispatcher) {
        val repository = createRepository()
        advanceUntilIdle()

        repository.start()

        verify(exactly = 1) { metronomeEngine.start(100) }
    }

    @Test
    fun `stop delegates to metronomeEngine`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.stop()

        verify(exactly = 1) { metronomeEngine.stop() }
    }

    @Test
    fun `toggle starts when not playing and stops when playing`() = runTest(testDispatcher) {
        val repository = createRepository()
        advanceUntilIdle()

        // When not playing -> toggle() starts
        isPlayingFlow.value = false
        repository.toggle()
        verify(exactly = 1) { metronomeEngine.start(100) }

        // When playing -> toggle() stops
        isPlayingFlow.value = true
        repository.toggle()
        verify(exactly = 1) { metronomeEngine.stop() }
    }

    @Test
    fun `setBpm clamps between 40 and 240, persists and informs engine`() = runTest(testDispatcher) {
        val repository = createRepository()

        // Below 40
        repository.setBpm(25)
        coVerify { preferencesDataSource.saveMetronomeBpm(40) }
        verify { metronomeEngine.setBpm(40) }

        // Above 240
        repository.setBpm(300)
        coVerify { preferencesDataSource.saveMetronomeBpm(240) }
        verify { metronomeEngine.setBpm(240) }

        // In range
        repository.setBpm(140)
        coVerify { preferencesDataSource.saveMetronomeBpm(140) }
        verify { metronomeEngine.setBpm(140) }
    }

    @Test
    fun `getSavedBpm returns value from preferences`() = runTest(testDispatcher) {
        coEvery { preferencesDataSource.getMetronomeBpm() } returns 112
        val repository = createRepository()

        val bpm = repository.getSavedBpm()
        assertEquals(112, bpm)
    }

    @Test
    fun `metronomeState combines engine state and beat updates accurately`() = runTest(testDispatcher) {
        val repository = createRepository()
        advanceUntilIdle()

        repository.metronomeState.test {
            val initial = awaitItem()
            assertEquals(false, initial.isPlaying)
            assertEquals(1, initial.currentBeat)

            isPlayingFlow.value = true
            val playing = awaitItem()
            assertEquals(true, playing.isPlaying)

            currentBeatFlow.value = 3
            val beat3 = awaitItem()
            assertEquals(3, beat3.currentBeat)
        }
    }
}
