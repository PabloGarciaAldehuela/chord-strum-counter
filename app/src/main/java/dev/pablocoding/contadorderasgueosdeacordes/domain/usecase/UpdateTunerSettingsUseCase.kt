package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import javax.inject.Inject

class UpdateTunerSettingsUseCase @Inject constructor(
    private val tunerRepository: TunerRepository
) {
    suspend operator fun invoke(transform: (TunerSettings) -> TunerSettings) =
        tunerRepository.updateSettings(transform)
}
