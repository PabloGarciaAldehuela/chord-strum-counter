package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import javax.inject.Inject

class PlayTunerSoundUseCase @Inject constructor(
    private val tunerRepository: TunerRepository
) {
    suspend fun referenceTone(frequencyHz: Double) = tunerRepository.playReferenceTone(frequencyHz)
    fun inTuneChime() = tunerRepository.playInTuneChime()
    fun stop() = tunerRepository.stopSounds()
}
