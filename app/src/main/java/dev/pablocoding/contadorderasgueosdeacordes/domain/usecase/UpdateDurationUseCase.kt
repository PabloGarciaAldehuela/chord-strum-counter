package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import javax.inject.Inject

class UpdateDurationUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(seconds: Int) {
        val clamped = seconds.coerceIn(15, 300)
        repository.savePreferredDuration(clamped)
    }
}
