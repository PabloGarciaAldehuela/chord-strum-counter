package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import dev.pablocoding.contadorderasgueosdeacordes.data.audio.MetronomeEngine
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.di.ApplicationScope
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.MetronomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetronomeRepositoryImpl @Inject constructor(
    private val metronomeEngine: MetronomeEngine,
    private val preferencesDataSource: PreferencesDataSource,
    @param:ApplicationScope private val appScope: CoroutineScope
) : MetronomeRepository {

    private val _bpm = MutableStateFlow(80)

    override val metronomeState: StateFlow<MetronomeState> = combine(
        metronomeEngine.isPlaying,
        _bpm,
        metronomeEngine.currentBeat
    ) { isPlaying, bpm, currentBeat ->
        MetronomeState(
            isPlaying = isPlaying,
            bpm = bpm,
            beatsPerMeasure = 4,
            currentBeat = currentBeat
        )
    }.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = MetronomeState()
    )

    init {
        appScope.launch {
            _bpm.value = preferencesDataSource.getMetronomeBpm()
        }
    }

    override suspend fun start() {
        metronomeEngine.start(_bpm.value)
    }

    override suspend fun stop() {
        metronomeEngine.stop()
    }

    override suspend fun toggle() {
        if (metronomeEngine.isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    override suspend fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(40, 240)
        _bpm.value = clamped
        preferencesDataSource.saveMetronomeBpm(clamped)
        metronomeEngine.setBpm(clamped)
    }

    override suspend fun getSavedBpm(): Int = preferencesDataSource.getMetronomeBpm()
}
