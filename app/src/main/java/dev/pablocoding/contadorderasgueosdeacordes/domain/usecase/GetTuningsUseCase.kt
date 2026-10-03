package dev.pablocoding.contadorderasgueosdeacordes.domain.usecase

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import javax.inject.Inject

class GetTuningsUseCase @Inject constructor() {
    operator fun invoke(): List<Tuning> = TuningLibrary.allTunings
    fun getTuning(id: String): Tuning = TuningLibrary.getTuning(id)
}
