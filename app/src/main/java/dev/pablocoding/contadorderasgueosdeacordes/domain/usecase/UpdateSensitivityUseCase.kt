package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import javax.inject.Inject

class UpdateSensitivityUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(value: Float) {
        repository.savePreferredSensitivity(value.coerceIn(0f, 1f))
    }
}
