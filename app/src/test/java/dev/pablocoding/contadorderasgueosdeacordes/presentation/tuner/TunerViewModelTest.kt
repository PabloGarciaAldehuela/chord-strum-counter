package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PracticeSessionActiveException
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerError
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetMetronomeStateUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetTunerSettingsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetTuningsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ObserveTunerPitchUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.PlayTunerSoundUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ToggleMetronomeUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.UpdateTunerSettingsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeTunerPitch: ObserveTunerPitchUseCase = mockk()
    private val getTunerSettings: GetTunerSettingsUseCase = mockk()
    private val updateTunerSettings: UpdateTunerSettingsUseCase = mockk()
    private val playTunerSound: PlayTunerSoundUseCase = mockk(relaxed = true)
    private val getMetronomeState: GetMetronomeStateUseCase = mockk()
    private val toggleMetronome: ToggleMetronomeUseCase = mockk(relaxed = true)
    private val observeTunerDiagnostics: dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.ObserveTunerDiagnosticsUseCase = mockk {
        every { this@mockk.invoke() } returns MutableStateFlow(false)
    }

    private val settings = MutableStateFlow(TunerSettings())
    private val metronomeState = MutableStateFlow(MetronomeState())
    private val samples = MutableSharedFlow<PitchSample>()
    private var subscriptions = 0

    @Before
    fun setUp() {
        every { getTunerSettings() } returns settings
        coEvery { updateTunerSettings(any()) } answers {
            val transform = firstArg<(TunerSettings) -> TunerSettings>()
            settings.value = transform(settings.value)
        }
        every { getMetronomeState() } returns metronomeState
        every { observeTunerPitch(any()) } answers {
            subscriptions++
            samples
        }
    }

    private fun createViewModel() = TunerViewModel(
        observeTunerPitch = observeTunerPitch,
        getTunerSettings = getTunerSettings,
        updateTunerSettings = updateTunerSettings,
        getTunings = GetTuningsUseCase(),
        playTunerSound = playTunerSound,
        getMetronomeState = getMetronomeState,
        toggleMetronome = toggleMetronome,
        observeTunerDiagnostics = observeTunerDiagnostics
    )

    private fun TestScope.collect(viewModel: TunerViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun sample(frequency: Double?, t: Long) =
        PitchSample(frequency, clarity = 0.98, level = 0.1, isHolding = false, isSignalPresent = frequency != null, timestampMs = t)

    private suspend fun emitSteady(frequency: Double, fromMs: Long = 0, untilMs: Long = 600) {
        for (t in fromMs..untilMs step 20) samples.emit(sample(frequency, t))
    }

    @Test
    fun `microphone is not opened until permission is granted`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        assertEquals(0, subscriptions)
        assertFalse(viewModel.uiState.value.hasMicPermission)

        viewModel.onPermissionResult(true)
        assertEquals(1, subscriptions)
        assertTrue(viewModel.uiState.value.hasMicPermission)
    }

    @Test
    fun `samples are evaluated against the selected tuning`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)

        samples.emit(sample(PitchMath.midiToFrequency(45) * 0.995, 0)) // A2, ~9 cents flat
        val reading = viewModel.uiState.value.reading
        assertEquals(1, reading.targetStringIndex)
        assertEquals(-8.7, reading.cents!!, 0.1)
        assertEquals(1, viewModel.uiState.value.activeStringIndex)
    }

    @Test
    fun `reaching pitch marks the string tuned, counts the event and chimes`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)

        emitSteady(PitchMath.midiToFrequency(40))
        val state = viewModel.uiState.value
        assertTrue(state.reading.isInTune)
        assertEquals(setOf(0), state.tunedStrings)
        assertEquals(1, state.inTuneEventCount)
        verify(exactly = 1) { playTunerSound.inTuneChime() }
    }

    @Test
    fun `no chime when the in-tune sound is disabled`() = runTest {
        settings.value = TunerSettings(inTuneSound = false)
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)

        emitSteady(PitchMath.midiToFrequency(40))
        assertEquals(1, viewModel.uiState.value.inTuneEventCount)
        verify(exactly = 0) { playTunerSound.inTuneChime() }
    }

    @Test
    fun `all strings tuned is reported once every string reached pitch`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)

        var t = 0L
        listOf(40, 45, 50, 55, 59, 64).forEach { midi ->
            emitSteady(PitchMath.midiToFrequency(midi), fromMs = t, untilMs = t + 600)
            t += 1000
        }
        assertTrue(viewModel.uiState.value.allStringsTuned)
    }

    @Test
    fun `tapping a string locks it and plays its reference tone`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)

        viewModel.onStringTapped(0)
        val state = viewModel.uiState.value
        assertFalse(state.isAutoMode)
        assertEquals(0, state.lockedStringIndex)
        coVerify { playTunerSound.referenceTone(match { kotlin.math.abs(it - 82.4069) < 0.001 }) }

        // Locked to low E even though the A string is ringing.
        samples.emit(sample(110.0, 0))
        assertEquals(0, viewModel.uiState.value.reading.targetStringIndex)
    }

    @Test
    fun `turning auto mode back on releases the lock`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onStringTapped(3)
        viewModel.onAutoModeChange(true)
        assertTrue(viewModel.uiState.value.isAutoMode)
        assertNull(viewModel.uiState.value.lockedStringIndex)
    }

    @Test
    fun `changing tuning or reference pitch clears tuned strings`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)
        emitSteady(PitchMath.midiToFrequency(40))
        assertEquals(setOf(0), viewModel.uiState.value.tunedStrings)

        viewModel.onSelectTuning("drop_d")
        assertEquals("drop_d", viewModel.uiState.value.tuning.id)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())

        emitSteady(PitchMath.midiToFrequency(38), fromMs = 1000, untilMs = 1600)
        assertEquals(setOf(0), viewModel.uiState.value.tunedStrings)
        viewModel.onReferencePitchChange(442)
        assertTrue(viewModel.uiState.value.tunedStrings.isEmpty())
        assertEquals(442, viewModel.uiState.value.settings.referencePitchHz)
    }

    @Test
    fun `reference pitch is clamped to the supported range`() = runTest {
        val viewModel = createViewModel()
        viewModel.onReferencePitchChange(400)
        assertEquals(TunerSettings.MIN_REFERENCE_PITCH_HZ, settings.value.referencePitchHz)
    }

    @Test
    fun `a running metronome is paused when the tuner opens`() = runTest {
        metronomeState.value = MetronomeState(isPlaying = true)
        val viewModel = createViewModel()
        collect(viewModel)
        coVerify { toggleMetronome(false) }
        assertTrue(viewModel.uiState.value.showMetronomePausedMessage)
        viewModel.onMetronomeMessageShown()
        assertFalse(viewModel.uiState.value.showMetronomePausedMessage)
    }

    @Test
    fun `a stopped metronome is left alone`() = runTest {
        createViewModel()
        coVerify(exactly = 0) { toggleMetronome(any()) }
    }

    @Test
    fun `revoked permission maps to a permission error and closes the mic`() = runTest {
        var attempts = 0
        every { observeTunerPitch(any()) } answers {
            attempts++
            if (attempts == 1) flow { throw AudioDetectionException.PermissionDenied() } else samples
        }
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)
        assertEquals(TunerError.MicrophonePermissionDenied, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.hasMicPermission)

        // User grants access again (e.g. from system Settings): error clears and the mic reopens.
        viewModel.onPermissionResult(true)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, attempts)
    }

    @Test
    fun `busy microphone shows an error and retry reopens it`() = runTest {
        var attempts = 0
        every { observeTunerPitch(any()) } answers {
            attempts++
            if (attempts == 1) flow { throw AudioDetectionException.MicrophoneUnavailable() } else samples
        }
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)
        assertEquals(TunerError.MicrophoneUnavailable, viewModel.uiState.value.error)

        viewModel.onRetry()
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, attempts)
    }

    @Test
    fun `practice session conflict is reported`() = runTest {
        every { observeTunerPitch(any()) } returns flow { throw PracticeSessionActiveException() }
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onPermissionResult(true)
        assertEquals(TunerError.PracticeSessionActive, viewModel.uiState.value.error)
    }

    @Test
    fun `settings toggles are forwarded`() = runTest {
        val viewModel = createViewModel()
        viewModel.onLeftHandedChange(true)
        viewModel.onInTuneSoundChange(false)
        assertTrue(settings.value.leftHanded)
        assertFalse(settings.value.inTuneSound)
    }
}
