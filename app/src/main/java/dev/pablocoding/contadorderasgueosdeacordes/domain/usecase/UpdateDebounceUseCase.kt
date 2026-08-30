package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import javax.inject.Inject

class UpdateDebounceUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(ms: Int) {
        repository.savePreferredDebounce(ms.coerceIn(100, 800))
    }
}
