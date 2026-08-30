package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MetronomeUseCasesTest {

    private lateinit var repository: MetronomeRepository
    private lateinit var toggleMetronomeUseCase: ToggleMetronomeUseCase
    private lateinit var updateMetronomeBpmUseCase: UpdateMetronomeBpmUseCase
    private lateinit var getMetronomeStateUseCase: GetMetronomeStateUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        toggleMetronomeUseCase = ToggleMetronomeUseCase(repository)
        updateMetronomeBpmUseCase = UpdateMetronomeBpmUseCase(repository)
        getMetronomeStateUseCase = GetMetronomeStateUseCase(repository)
    }

    @Test
    fun `ToggleMetronomeUseCase toggles repository when forceState is null`() = runTest {
        toggleMetronomeUseCase()
        coVerify(exactly = 1) { repository.toggle() }
    }

    @Test
    fun `ToggleMetronomeUseCase starts repository when forceState is true`() = runTest {
        toggleMetronomeUseCase(forceState = true)
        coVerify(exactly = 1) { repository.start() }
    }

    @Test
    fun `ToggleMetronomeUseCase stops repository when forceState is false`() = runTest {
        toggleMetronomeUseCase(forceState = false)
        coVerify(exactly = 1) { repository.stop() }
    }

    @Test
    fun `UpdateMetronomeBpmUseCase clamps bpm between 40 and 240`() = runTest {
        updateMetronomeBpmUseCase(20)
        coVerify { repository.setBpm(40) }

        updateMetronomeBpmUseCase(120)
        coVerify { repository.setBpm(120) }

        updateMetronomeBpmUseCase(300)
        coVerify { repository.setBpm(240) }
    }

    @Test
    fun `GetMetronomeStateUseCase returns repository metronomeState flow`() {
        val stateFlow = MutableStateFlow(MetronomeState(bpm = 100))
        every { repository.metronomeState } returns stateFlow

        val result = getMetronomeStateUseCase()
        assertEquals(100, result.value.bpm)
    }
}
