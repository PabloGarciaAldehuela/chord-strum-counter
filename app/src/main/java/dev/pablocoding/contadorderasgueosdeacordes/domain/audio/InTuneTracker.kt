package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerTolerance
import kotlin.math.abs

/**
 * In-tune state with hysteresis: entering requires staying within the tolerance for [holdMs]
 * continuously; leaving requires drifting beyond tolerance + [exitMarginCents].
 */
class InTuneTracker(
    private val holdMs: Long = 400,
    private val exitMarginCents: Double = TunerTolerance.EXIT_MARGIN_CENTS
) {
    var isInTune: Boolean = false
        private set

    private var withinSinceMs: Long? = null

    fun update(cents: Double, nowMs: Long, toleranceCents: Double): Boolean {
        val deviation = abs(cents)
        if (isInTune) {
            if (deviation > toleranceCents + exitMarginCents) {
                isInTune = false
                withinSinceMs = null
            }
        } else if (deviation <= toleranceCents) {
            val since = withinSinceMs ?: nowMs.also { withinSinceMs = it }
            if (nowMs - since >= holdMs) isInTune = true
        } else {
            withinSinceMs = null
        }
        return isInTune
    }

    /**
     * Resets the pending in-tune qualification timer while preserving an already-confirmed
     * [isInTune] state.
     */
    fun resetPendingTimer() {
        withinSinceMs = null
    }

    fun reset() {
        isInTune = false
        withinSinceMs = null
    }
}
