package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticNote
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticWheelData
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.GuitarFretPosition
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.PlayTunerSoundUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChromaticClockUiState(
    val selectedPitchClass: Int = 0, // C by default
    val notationMode: NoteNotationMode = NoteNotationMode.DUAL,
    val selectedTab: Int = 0, // 0 = Fretboard, 1 = Intervals, 2 = Theory Rules
    val walkStringIndex: Int = 0, // 0 = 6th string (Low E)
    val walkFret: Int = 0,
    val calcRootPitchClass: Int = 0, // C
    val calcTargetPitchClass: Int = 7, // G (5th)
    val isAudioEnabled: Boolean = true
) {
    val selectedNote: ChromaticNote
        get() = ChromaticWheelData.NOTES[selectedPitchClass.coerceIn(0, 11)]

    val fretPositions: List<GuitarFretPosition>
        get() = ChromaticWheelData.fretPositionsFor(selectedPitchClass)

    val semitoneDistance: Int
        get() = ChromaticWheelData.semitonesBetween(calcRootPitchClass, calcTargetPitchClass)

    val intervalInfo: Pair<String, String>
        get() = ChromaticWheelData.intervalName(semitoneDistance)
}

@HiltViewModel
class ChromaticClockViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val playTunerSoundUseCase: PlayTunerSoundUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChromaticClockUiState())
    val uiState: StateFlow<ChromaticClockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedMode = preferencesDataSource.getNoteClockNotation()
            _uiState.update { it.copy(notationMode = savedMode) }
        }
    }

    fun selectPitchClass(pitchClass: Int, playAudio: Boolean = true) {
        val normalized = Math.floorMod(pitchClass, 12)
        _uiState.update { it.copy(selectedPitchClass = normalized) }
        if (playAudio && _uiState.value.isAudioEnabled) {
            playToneForPitchClass(normalized)
        }
    }

    fun setNotationMode(mode: NoteNotationMode) {
        _uiState.update { it.copy(notationMode = mode) }
        viewModelScope.launch {
            preferencesDataSource.saveNoteClockNotation(mode)
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex.coerceIn(0, 2)) }
    }

    fun setWalkString(stringIndex: Int) {
        val safeString = stringIndex.coerceIn(0, 5)
        val currentFret = _uiState.value.walkFret
        val pitchClass = ChromaticWheelData.pitchClassAt(safeString, currentFret)
        _uiState.update { it.copy(walkStringIndex = safeString, selectedPitchClass = pitchClass) }
        if (_uiState.value.isAudioEnabled) {
            playToneForPitchClass(pitchClass)
        }
    }

    fun setWalkFret(fret: Int) {
        val safeFret = fret.coerceIn(0, 12)
        val currentString = _uiState.value.walkStringIndex
        val pitchClass = ChromaticWheelData.pitchClassAt(currentString, safeFret)
        _uiState.update { it.copy(walkFret = safeFret, selectedPitchClass = pitchClass) }
        if (_uiState.value.isAudioEnabled) {
            playToneForPitchClass(pitchClass)
        }
    }

    fun setCalcRoot(pitchClass: Int) {
        _uiState.update { it.copy(calcRootPitchClass = Math.floorMod(pitchClass, 12)) }
    }

    fun setCalcTarget(pitchClass: Int) {
        _uiState.update { it.copy(calcTargetPitchClass = Math.floorMod(pitchClass, 12)) }
    }

    fun toggleAudio() {
        _uiState.update { it.copy(isAudioEnabled = !it.isAudioEnabled) }
    }

    fun playCurrentNote() {
        playToneForPitchClass(_uiState.value.selectedPitchClass)
    }

    private fun playToneForPitchClass(pitchClass: Int) {
        viewModelScope.launch {
            val note = ChromaticWheelData.NOTES[pitchClass.coerceIn(0, 11)]
            val frequencyHz = PitchMath.midiToFrequency(note.baseMidi)
            playTunerSoundUseCase.referenceTone(frequencyHz)
        }
    }
}
