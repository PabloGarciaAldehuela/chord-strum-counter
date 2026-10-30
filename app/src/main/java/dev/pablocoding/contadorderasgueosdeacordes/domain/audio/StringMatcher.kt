package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs

/**
 * Auto mode: decides which string of the tuning the user is playing.
 *
 * Uses hysteresis so a string sitting between two targets doesn't make the target flap:
 * another string must be at least [switchMarginCents] closer for [confirmFrames] consecutive
 * frames. When the current target is more than [immediateSwitchCents] away the user has clearly
 * plucked a different string, so the switch happens at once.
 */
class StringMatcher(
    private val switchMarginCents: Double = 30.0,
    private val confirmFrames: Int = 3,
    private val immediateSwitchCents: Double = 150.0
) {
    var currentIndex: Int? = null
        private set

    private var pendingIndex = -1
    private var pendingCount = 0

    /**
     * @param midi Fractional MIDI number of the detected pitch.
     * @param strings Target MIDI notes of the tuning (must not be empty).
     * @return Index of the matched string.
     */
    fun match(midi: Double, strings: List<Int>): Int {
        require(strings.isNotEmpty()) { "A tuning needs at least one string to match against" }

        var best = 0
        for (i in 1 until strings.size) {
            if (abs(midi - strings[i]) < abs(midi - strings[best])) best = i
        }

        val current = currentIndex?.takeIf { it in strings.indices }
        if (current == null || current == best) {
            currentIndex = best
            clearPending()
            return best
        }

        val currentDistance = abs(midi - strings[current]) * 100.0
        val gain = currentDistance - abs(midi - strings[best]) * 100.0
        if (gain < switchMarginCents) {
            clearPending()
            return current
        }
        if (currentDistance > immediateSwitchCents) {
            currentIndex = best
            clearPending()
            return best
        }

        if (pendingIndex == best) pendingCount++ else {
            pendingIndex = best
            pendingCount = 1
        }
        if (pendingCount >= confirmFrames) {
            currentIndex = best
            clearPending()
            return best
        }
        return current
    }

    fun reset() {
        currentIndex = null
        clearPending()
    }

    private fun clearPending() {
        pendingIndex = -1
        pendingCount = 0
    }
}
