package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetMetronomeStateUseCase @Inject constructor(
    private val metronomeRepository: MetronomeRepository
) {
    operator fun invoke(): StateFlow<MetronomeState> = metronomeRepository.metronomeState
}
