package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PracticeSessionActiveException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TunerAnalysisContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.TunerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Streams stabilized pitch samples from the microphone. Refuses to start while a practice session
 * is listening for strums, so two recorders never compete for the microphone.
 */
class ObserveTunerPitchUseCase @Inject constructor(
    private val tunerRepository: TunerRepository,
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(context: StateFlow<TunerAnalysisContext>): Flow<PitchSample> = flow {
        if (sessionRepository.sessionFlow.first().isRunning) throw PracticeSessionActiveException()
        emitAll(tunerRepository.pitchSamples(context))
    }
}
