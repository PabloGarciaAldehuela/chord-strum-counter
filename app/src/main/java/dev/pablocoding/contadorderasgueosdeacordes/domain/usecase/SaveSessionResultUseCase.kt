package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionHistoryRepository
import javax.inject.Inject

class SaveSessionResultUseCase @Inject constructor(
    private val repository: SessionHistoryRepository
) {
    suspend operator fun invoke(result: SessionResult) {
        repository.saveSession(result)
    }
}
