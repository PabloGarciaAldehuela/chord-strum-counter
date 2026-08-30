package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary
import javax.inject.Inject

class GetChordLibraryUseCase @Inject constructor() {
    operator fun invoke(): List<Chord> = ChordLibrary.allChords
    fun getChord(name: String): Chord? = ChordLibrary.getChord(name)
}
