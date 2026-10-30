package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.min

/**
 * Room noise level by minimum statistics: the floor is the lowest analysis level seen over the
 * last [blocks] × [blockMs] (≈1.5 s), so speech bursts, plucks and other transients never inflate
 * it, while a room that gets louder (a fan switching on) is followed within ~1.5 s.
 *
 * The first [warmupMs] of capture are ignored — phones deliver silence or a ramp right after the
 * microphone opens, and learning that as the floor would leave the gate wide open (the bug behind
 * background tones being displayed as notes).
 *
 * Updates are frozen while a note rings so a long sustain is never mistaken for the room.
 */
class NoiseFloorEstimator(
    private val blockMs: Long = 500,
    private val blocks: Int = 3,
    private val warmupMs: Long = 300
) {
    private val blockMinima = DoubleArray(blocks) { Double.MAX_VALUE }
    private var blockIndex = 0
    private var blockStartMs = Long.MIN_VALUE
    private var currentMin = Double.MAX_VALUE
    private var startMs = Long.MIN_VALUE

    /** Current floor estimate, or null while warming up. */
    var floor: Double? = null
        private set

    fun update(level: Double, nowMs: Long, frozen: Boolean = false): Double? {
        if (startMs == Long.MIN_VALUE) startMs = nowMs
        if (nowMs - startMs < warmupMs || frozen) return floor
        if (blockStartMs == Long.MIN_VALUE) blockStartMs = nowMs
        currentMin = min(currentMin, level)
        if (nowMs - blockStartMs >= blockMs) {
            blockMinima[blockIndex] = currentMin
            blockIndex = (blockIndex + 1) % blocks
            currentMin = Double.MAX_VALUE
            blockStartMs = nowMs
        }
        var lowest = currentMin
        for (m in blockMinima) lowest = min(lowest, m)
        floor = lowest
        return floor
    }

    fun reset() {
        blockMinima.fill(Double.MAX_VALUE)
        blockIndex = 0
        blockStartMs = Long.MIN_VALUE
        currentMin = Double.MAX_VALUE
        startMs = Long.MIN_VALUE
        floor = null
    }
}
