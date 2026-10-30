package dev.pablocoding.contadorderasgueosdeacordes.domain.repository

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.TunerAnalysisContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TunerRepository {
    val settings: StateFlow<TunerSettings>

    /**
     * Cold flow: opens the microphone on collection and releases it on cancellation, so it only
     * listens while the tuner screen is visible.
     *
     * @param context Current tuning targets / locked string; read on every analysis hop, so tuning
     *        changes apply without reopening the microphone.
     * @throws dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
     */
    fun pitchSamples(context: StateFlow<TunerAnalysisContext>): Flow<PitchSample>

    /** True while a developer diagnostics capture is recording the microphone to disk. */
    val isDiagnosticsRecording: StateFlow<Boolean>

    suspend fun updateSettings(transform: (TunerSettings) -> TunerSettings)

    suspend fun playReferenceTone(frequencyHz: Double)
    fun playInTuneChime()
    fun stopSounds()
}
