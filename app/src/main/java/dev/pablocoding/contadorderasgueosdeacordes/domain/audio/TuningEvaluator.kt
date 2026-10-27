package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import kotlin.math.abs

/**
 * Maps stabilized pitch samples onto a tuning: picks the target (auto-matched string, locked
 * string, or nearest semitone for chromatic), computes cents, and tracks the in-tune state.
 *
 * Stateful (string hysteresis, in-tune timer, noise hint), so call [reset] whenever the tuning,
 * reference pitch or locked string changes.
 */
/** The harmonic analysis searches ±150¢ around each string; a hint further away than that is stale. */
private const val HINT_MAX_CENTS = 150.0

class TuningEvaluator(
    private val stringMatcher: StringMatcher = StringMatcher(),
    private val inTuneTracker: InTuneTracker = InTuneTracker(),
    private val noiseHintDelayMs: Long = 3000
) {
    private var lastTargetMidi: Int? = null
    private var lastTargetIndex: Int? = null
    private var unusableSignalSinceMs: Long? = null

    fun evaluate(
        sample: PitchSample,
        tuning: Tuning,
        referencePitchHz: Double,
        lockedStringIndex: Int?,
        toleranceCents: Double
    ): TunerReading {
        val showNoiseHint = updateNoiseHint(sample)
        val frequency = sample.frequencyHz
        if (frequency == null) {
            inTuneTracker.reset()
            return TunerReading(
                targetMidi = lockedStringIndex?.let { tuning.strings.getOrNull(it) },
                targetStringIndex = lockedStringIndex?.takeIf { it in tuning.strings.indices },
                signalLevel = sample.level,
                showNoiseHint = showNoiseHint,
                isInputSilenced = sample.isInputSilenced,
                timestampMs = sample.timestampMs
            )
        }

        val midi = PitchMath.frequencyToMidi(frequency, referencePitchHz)
        val detectedMidi = Math.round(midi).toInt()
        val (targetIndex, targetMidi) = when {
            tuning.isChromatic -> null to detectedMidi
            lockedStringIndex != null && lockedStringIndex in tuning.strings.indices ->
                lockedStringIndex to tuning.strings[lockedStringIndex]
            sample.isHolding && lastTargetIndex != null && lastTargetIndex in tuning.strings.indices ->
                lastTargetIndex to tuning.strings[lastTargetIndex!!]
            // The pipeline identified the plucked string from its harmonics: trust that over
            // nearest-pitch matching (which picks the wrong string when one is far out of tune).
            sample.stringHint != null && sample.stringHint in tuning.strings.indices &&
                abs(midi - tuning.strings[sample.stringHint]) * 100.0 <= HINT_MAX_CENTS ->
                sample.stringHint to tuning.strings[sample.stringHint]
            else -> stringMatcher.match(midi, tuning.strings).let { it to tuning.strings[it] }
        }

        if (targetMidi != lastTargetMidi || targetIndex != lastTargetIndex) inTuneTracker.reset()
        lastTargetMidi = targetMidi
        lastTargetIndex = targetIndex

        val cents = PitchMath.cents(frequency, PitchMath.midiToFrequency(targetMidi, referencePitchHz))
        val wasInTune = inTuneTracker.isInTune
        // A held (decaying) value is frozen, and the attack glide is still sharp: keep the state
        // without advancing the timer, and reset any pending timer so held/unsettled frames
        // cannot bridge disconnected readings.
        val isInTune = if (sample.isHolding || !sample.isSettled) {
            inTuneTracker.resetPendingTimer()
            wasInTune
        } else {
            inTuneTracker.update(cents, sample.timestampMs, toleranceCents)
        }

        return TunerReading(
            frequencyHz = frequency,
            detectedMidi = detectedMidi,
            targetMidi = targetMidi,
            targetStringIndex = targetIndex,
            cents = cents,
            isInTune = isInTune,
            justBecameInTune = isInTune && !wasInTune,
            isHolding = sample.isHolding,
            signalLevel = sample.level,
            showNoiseHint = showNoiseHint,
            isInputSilenced = sample.isInputSilenced,
            timestampMs = sample.timestampMs
        )
    }

    fun reset() {
        stringMatcher.reset()
        inTuneTracker.reset()
        lastTargetMidi = null
        lastTargetIndex = null
        unusableSignalSinceMs = null
    }

    private fun updateNoiseHint(sample: PitchSample): Boolean {
        val unusable = sample.isSignalPresent && sample.frequencyHz == null
        if (!unusable) {
            unusableSignalSinceMs = null
            return false
        }
        val since = unusableSignalSinceMs ?: sample.timestampMs.also { unusableSignalSinceMs = it }
        return sample.timestampMs - since >= noiseHintDelayMs
    }
}
