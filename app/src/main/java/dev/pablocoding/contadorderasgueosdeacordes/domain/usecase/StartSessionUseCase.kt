package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(durationSeconds: Int) {
        repository.startSession(durationSeconds)
    }
}
