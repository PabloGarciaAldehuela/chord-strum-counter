package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.PlayTunerSoundUseCase
import dev.pablocoding.contadorderasgueosdeacordes.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChromaticClockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesDataSource: PreferencesDataSource = mockk(relaxed = true)
    private val playTunerSoundUseCase: PlayTunerSoundUseCase = mockk(relaxed = true)

    private lateinit var viewModel: ChromaticClockViewModel

    @Before
    fun setUp() {
        coEvery { preferencesDataSource.getNoteClockNotation() } returns NoteNotationMode.DUAL
        viewModel = ChromaticClockViewModel(preferencesDataSource, playTunerSoundUseCase)
    }

    @Test
    fun defaultStateLoadsCorrectly() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedPitchClass) // C
        assertEquals("C", state.selectedNote.nameLetters)
        assertEquals("Do", state.selectedNote.nameSolfege)
        assertEquals(NoteNotationMode.DUAL, state.notationMode)
        assertTrue(state.isAudioEnabled)
        assertEquals(0, state.selectedTab)
    }

    @Test
    fun selectPitchClassUpdatesStateAndPlaysAudio() = runTest {
        advanceUntilIdle()

        // Select note 4 (E)
        viewModel.selectPitchClass(4, playAudio = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.selectedPitchClass)
        assertEquals("E", state.selectedNote.nameLetters)
        assertTrue(state.selectedNote.isGluedWithNext) // E is glued to F!

        coVerify { playTunerSoundUseCase.referenceTone(any()) }
    }

    @Test
    fun setNotationModeUpdatesStateAndSavesToPreferences() = runTest {
        advanceUntilIdle()

        viewModel.setNotationMode(NoteNotationMode.SOLFEGE)
        advanceUntilIdle()

        assertEquals(NoteNotationMode.SOLFEGE, viewModel.uiState.value.notationMode)
        coVerify { preferencesDataSource.saveNoteClockNotation(NoteNotationMode.SOLFEGE) }
    }

    @Test
    fun walkFretOnLowEUpdatesPitchClass() = runTest {
        advanceUntilIdle()

        // On 6th string (Low E, pitch 4): Fret 1 should be F (pitch 5)
        viewModel.setWalkString(0)
        viewModel.setWalkFret(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.walkFret)
        assertEquals(5, state.selectedPitchClass)
        assertEquals("F", state.selectedNote.nameLetters)
    }

    @Test
    fun intervalCalculatorCalculatesSemitonesBetweenRootAndTarget() = runTest {
        advanceUntilIdle()

        // Root = C (0), Target = G (7)
        viewModel.setCalcRoot(0)
        viewModel.setCalcTarget(7)

        val state = viewModel.uiState.value
        assertEquals(7, state.semitoneDistance)
        assertEquals("Perfect 5th", state.intervalInfo.first)
    }

    @Test
    fun toggleAudioMutesTonePlayback() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAudioEnabled)

        viewModel.toggleAudio()
        assertFalse(viewModel.uiState.value.isAudioEnabled)
    }
}
