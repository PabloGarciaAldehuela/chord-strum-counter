package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import javax.inject.Inject

class UpdateSelectedChordsUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(chords: List<String>) {
        val validChords = if (chords.isEmpty()) listOf("A", "D") else chords
        repository.savePreferredChords(validChords)
    }
}
