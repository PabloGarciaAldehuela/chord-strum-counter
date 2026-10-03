package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Whether a developer diagnostics capture is recording the microphone right now. */
class ObserveTunerDiagnosticsUseCase @Inject constructor(
    private val tunerRepository: TunerRepository
) {
    operator fun invoke(): StateFlow<Boolean> = tunerRepository.isDiagnosticsRecording
}
