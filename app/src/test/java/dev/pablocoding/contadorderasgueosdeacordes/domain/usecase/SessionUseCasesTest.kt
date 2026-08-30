package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        startSessionUseCase = StartSessionUseCase(repository)
        stopSessionUseCase = StopSessionUseCase(repository)
        registerTransitionUseCase = RegisterTransitionUseCase(repository)
        updateDurationUseCase = UpdateDurationUseCase(repository)
        updateSensitivityUseCase = UpdateSensitivityUseCase(repository)
        updateDebounceUseCase = UpdateDebounceUseCase(repository)
    }

    @Test
    fun `StartSessionUseCase invokes repository startSession with given duration`() = runTest {
        startSessionUseCase(90)
        coVerify(exactly = 1) { repository.startSession(90) }
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
        updateDurationUseCase(5) // below min
        coVerify { repository.savePreferredDuration(15) }

        updateDurationUseCase(60) // valid
        coVerify { repository.savePreferredDuration(60) }

        updateDurationUseCase(500) // above max
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
}
