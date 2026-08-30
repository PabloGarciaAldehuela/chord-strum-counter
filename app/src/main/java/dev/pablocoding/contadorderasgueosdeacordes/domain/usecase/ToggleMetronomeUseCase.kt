package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import javax.inject.Inject

class ToggleMetronomeUseCase @Inject constructor(
    private val metronomeRepository: MetronomeRepository
) {
    suspend operator fun invoke(forceState: Boolean? = null) {
        when (forceState) {
            true -> metronomeRepository.start()
            false -> metronomeRepository.stop()
            null -> metronomeRepository.toggle()
        }
    }
}
