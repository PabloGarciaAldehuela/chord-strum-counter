package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import javax.inject.Inject

class UpdateMetronomeBpmUseCase @Inject constructor(
    private val metronomeRepository: MetronomeRepository
) {
    suspend operator fun invoke(bpm: Int) {
        val clamped = bpm.coerceIn(40, 240)
        metronomeRepository.setBpm(clamped)
    }
}
