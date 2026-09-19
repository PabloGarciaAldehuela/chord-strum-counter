package dev.pablocoding.contadorderasgueosdeacordes.data.audio

/**
 * Abstraction for querying monotonic elapsed time.
 * Decouples system clocks from audio processing logic to ensure deterministic unit testing.
 */
fun interface TimeSource {
    fun elapsedRealtime(): Long
}
