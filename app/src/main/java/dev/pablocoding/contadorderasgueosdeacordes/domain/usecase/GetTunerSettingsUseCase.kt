package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetTunerSettingsUseCase @Inject constructor(
    private val tunerRepository: TunerRepository
) {
    operator fun invoke(): StateFlow<TunerSettings> = tunerRepository.settings
}
