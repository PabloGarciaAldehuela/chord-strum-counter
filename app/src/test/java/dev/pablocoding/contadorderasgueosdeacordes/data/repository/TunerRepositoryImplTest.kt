package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import dev.pablocoding.contadorderasgueosdeacordes.data.audio.PcmInputStream
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerMicrophone
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerSoundEngine
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class TunerRepositoryImplTest {

    private val preferencesDataSource: PreferencesDataSource = mockk(relaxed = true)
    private val microphone: TunerMicrophone = mockk()
    private val soundEngine: TunerSoundEngine = mockk(relaxed = true)
    private val diagnostics: dev.pablocoding.contadorderasgueosdeacordes.data.audio.TunerDiagnostics = mockk {
        every { startSession(any(), any()) } returns null
        every { isRecording } returns MutableStateFlow(false)
    }
    private val appScope = CoroutineScope(UnconfinedTestDispatcher())

    /** Fake capture producing either a sine at [frequencyHz], digital silence, or a read error. */
    private class FakeStream(
        override val sampleRate: Int = 48_000,
        private val frequencyHz: Double? = null,
        private val errorCode: Int? = null,
        private val silenced: Boolean = false
    ) : PcmInputStream {
        var position = 0L
        var closed = false

        override fun read(buffer: FloatArray, count: Int): Int {
            errorCode?.let { return it }
            for (i in 0 until count) {
                // Half a second of room noise, then the "pluck" (the tuner only reads plucked notes).
                val t = position + i
                val note = if (t >= sampleRate / 2) frequencyHz?.let { 0.3 * sin(2 * PI * it * t / sampleRate) } ?: 0.0 else 0.0
                val hiss = if (frequencyHz != null) 0.0005 * sin(t * 1.7) * sin(t * 0.31) else 0.0
                buffer[i] = (note + hiss).toFloat()
            }
            position += count
            return count
        }

        override fun isSilenced() = silenced

        override fun close() {
            closed = true
        }
    }

    @Before
    fun setUp() {
        every { soundEngine.isDetectionMuted() } returns false
        coEvery { preferencesDataSource.getTunerSettings(any()) } returns TunerSettings(tuningId = "drop_d", noteNaming = NoteNaming.LETTERS)
    }

    private val context = kotlinx.coroutines.flow.MutableStateFlow(
        dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TunerAnalysisContext(
            dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary.standard.strings.map { PitchMath.midiToFrequency(it) }
        )
    )

    private fun createRepository() = TunerRepositoryImpl(preferencesDataSource, microphone, soundEngine, diagnostics, appScope)

    @Test
    fun `loads stored settings on creation`() = runTest {
        val repository = createRepository()
        assertEquals("drop_d", repository.settings.value.tuningId)
    }

    @Test
    fun `streams the detected pitch of the microphone signal`() = runBlocking {
        val a2 = PitchMath.midiToFrequency(45)
        val stream = FakeStream(frequencyHz = a2)
        every { microphone.open() } returns stream

        val sample = withTimeout(5_000) {
            createRepository().pitchSamples(context).first { it.frequencyHz != null }
        }
        assertEquals(0.0, PitchMath.cents(sample.frequencyHz!!, a2), 1.0)
        assertTrue("Microphone must be released when collection stops", stream.closed)
    }

    @Test
    fun `reports when the OS silences the capture for another app`() = runBlocking {
        every { microphone.open() } returns FakeStream(frequencyHz = null, silenced = true)
        val sample = withTimeout(5_000) { createRepository().pitchSamples(context).first() }
        assertTrue(sample.isInputSilenced)
    }

    @Test
    fun `quiet room with exact digital zeros is not mistaken for a silenced mic`() = runBlocking {
        every { microphone.open() } returns FakeStream(frequencyHz = null, silenced = false)
        val samples = withTimeout(5_000) {
            val list = mutableListOf<dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample>()
            createRepository().pitchSamples(context).first { list.add(it); list.size >= 150 }
            list
        }
        assertTrue(samples.none { it.isInputSilenced })
    }

    @Test
    fun `read errors surface as recording failures and release the microphone`() = runBlocking {
        val stream = FakeStream(errorCode = -3)
        every { microphone.open() } returns stream
        val result = runCatching { createRepository().pitchSamples(context).first() }
        assertTrue(result.exceptionOrNull() is AudioDetectionException.RecordingFailed)
        assertTrue(stream.closed)
    }

    @Test
    fun `updating settings persists them and clamps the reference pitch`() = runTest {
        val repository = createRepository()
        repository.updateSettings { it.copy(referencePitchHz = 500) }
        assertEquals(TunerSettings.MAX_REFERENCE_PITCH_HZ, repository.settings.value.referencePitchHz)
        coVerify { preferencesDataSource.saveTunerSettings(match { it.referencePitchHz == TunerSettings.MAX_REFERENCE_PITCH_HZ }) }
    }
}
