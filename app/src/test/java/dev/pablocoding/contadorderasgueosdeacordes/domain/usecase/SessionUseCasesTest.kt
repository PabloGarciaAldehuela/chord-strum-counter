package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionUseCasesTest {

    private lateinit var repository: SessionRepository
    private lateinit var startSessionUseCase: StartSessionUseCase
    private lateinit var stopSessionUseCase: StopSessionUseCase
    private lateinit var registerTransitionUseCase: RegisterTransitionUseCase
    private lateinit var updateDurationUseCase: UpdateDurationUseCase
    private lateinit var updateSensitivityUseCase: UpdateSensitivityUseCase
    private lateinit var updateDebounceUseCase: UpdateDebounceUseCase
    private lateinit var getSelectedChordsUseCase: GetSelectedChordsUseCase
    private lateinit var updateSelectedChordsUseCase: UpdateSelectedChordsUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        startSessionUseCase = StartSessionUseCase(repository)
        stopSessionUseCase = StopSessionUseCase(repository)
        registerTransitionUseCase = RegisterTransitionUseCase(repository)
        updateDurationUseCase = UpdateDurationUseCase(repository)
        updateSensitivityUseCase = UpdateSensitivityUseCase(repository)
        updateDebounceUseCase = UpdateDebounceUseCase(repository)
        getSelectedChordsUseCase = GetSelectedChordsUseCase(repository)
        updateSelectedChordsUseCase = UpdateSelectedChordsUseCase(repository)
    }

    @Test
    fun `StartSessionUseCase invokes repository startSession with given duration and chords`() = runTest {
        startSessionUseCase(90, listOf("A", "D", "E"))
        coVerify(exactly = 1) { repository.startSession(90, listOf("A", "D", "E")) }
    }

    @Test
    fun `StopSessionUseCase invokes repository stopSession`() = runTest {
        stopSessionUseCase()
        coVerify(exactly = 1) { repository.stopSession() }
    }

    @Test
    fun `RegisterTransitionUseCase invokes repository registerTransition`() = runTest {
        registerTransitionUseCase()
        coVerify(exactly = 1) { repository.registerTransition() }
    }

    @Test
    fun `UpdateDurationUseCase clamps duration between 15 and 300 seconds and saves`() = runTest {
        updateDurationUseCase(5)
        coVerify { repository.savePreferredDuration(15) }

        updateDurationUseCase(60)
        coVerify { repository.savePreferredDuration(60) }

        updateDurationUseCase(500)
        coVerify { repository.savePreferredDuration(300) }
    }

    @Test
    fun `UpdateSensitivityUseCase clamps value between 0 and 1`() = runTest {
        updateSensitivityUseCase(-0.5f)
        coVerify { repository.savePreferredSensitivity(0f) }

        updateSensitivityUseCase(0.75f)
        coVerify { repository.savePreferredSensitivity(0.75f) }

        updateSensitivityUseCase(1.5f)
        coVerify { repository.savePreferredSensitivity(1f) }
    }

    @Test
    fun `UpdateDebounceUseCase clamps value between 100 and 800 ms`() = runTest {
        updateDebounceUseCase(50)
        coVerify { repository.savePreferredDebounce(100) }

        updateDebounceUseCase(350)
        coVerify { repository.savePreferredDebounce(350) }

        updateDebounceUseCase(1200)
        coVerify { repository.savePreferredDebounce(800) }
    }

    @Test
    fun `GetSelectedChordsUseCase returns preferred chords from repository`() = runTest {
        coEvery { repository.getPreferredChords() } returns listOf("C", "G", "Am")
        val chords = getSelectedChordsUseCase()
        assertEquals(listOf("C", "G", "Am"), chords)
    }

    @Test
    fun `UpdateSelectedChordsUseCase saves chords to repository or defaults if empty`() = runTest {
        updateSelectedChordsUseCase(listOf("E", "A", "B7"))
        coVerify { repository.savePreferredChords(listOf("E", "A", "B7")) }

        updateSelectedChordsUseCase(emptyList())
        coVerify { repository.savePreferredChords(listOf("A", "D")) }
    }
}
