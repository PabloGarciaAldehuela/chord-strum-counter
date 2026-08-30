package dev.pablocoding.contadorderasgueosdeacordes.domain.repository

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.MetronomeState
import kotlinx.coroutines.flow.StateFlow

interface MetronomeRepository {
    val metronomeState: StateFlow<MetronomeState>
    suspend fun start()
    suspend fun stop()
    suspend fun toggle()
    suspend fun setBpm(bpm: Int)
    suspend fun getSavedBpm(): Int
}
