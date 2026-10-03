package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Streaming tuner front-end: accepts arbitrary chunks of mono float PCM and, every [hopSize]
 * samples (~21 ms), returns a stabilized [PitchSample].
 *
 * Signal chain:
 * 1. High-pass just below the tuning's lowest string (room rumble, mains hum) → learned
 *    background-tone notches → 4th-order 1.5 kHz low-pass (most broadband noise is above the
 *    guitar's pitch range) → ring buffer.
 * 2. **Time-domain MPM** on ~85 ms (and ~170 ms when not confident): fast and precise on clean
 *    notes; octave errors corrected with the tuning as a prior ([OctaveResolver]) and results
 *    outside the tuning's range dropped.
 * 3. **Harmonic spectrum analysis** ([HarmonicSpectrumDetector], ~340 ms) every other hop: finds
 *    weak strings buried in broadband noise and tells which string is sounding.
 * 4. **String choice per note:** the string whose harmonic evidence *rose* most at the latest pluck
 *    onset wins, so a string still ringing from before can't hijack the reading.
 * 5. **Fusion:** MPM's value when it agrees with the identified string, the spectral value when
 *    MPM is unsure, MPM alone only when it's very clear (or in chromatic mode).
 * 6. [PitchStabilizer] for notes, settling and smoothing.
 *
 * Time is derived from the sample count, so behaviour is deterministic and testable.
 */
class PitchTracker(
    val sampleRate: Int,
    private val stabilizer: PitchStabilizer = PitchStabilizer(),
    context: TunerAnalysisContext = TunerAnalysisContext.CHROMATIC
) {
    companion object {
        private const val WINDOW_SECONDS = 0.08
        private const val LOW_PASS_HZ = 1500.0
        private const val CLICK_HIGH_PASS_HZ = 2000.0
        private const val NOTCH_Q = 30.0
        /** Short-window clarity above which the long window isn't worth computing. */
        private const val CONFIDENT_CLARITY = 0.97
        /** MPM may stand alone (no harmonic confirmation) only above this clarity. */
        private const val SOLO_CLARITY = 0.93
        /** MPM leads the fused value when the harmonic analysis confirms it, above this clarity. */
        private const val ASSISTED_CLARITY = 0.93
        private const val AGREE_CENTS = 1.5
        private const val SPECTRAL_CONFIDENCE = 0.9
        private const val SPECTRAL_EVERY_HOPS = 2
        /** A string must gain this much harmonic score at the onset to count as "just plucked". */
        private const val MIN_NOVELTY = 10.0
        private const val RISE_FRACTION = 0.15
        private const val CONTINUITY_VOTES = 2.0
        private const val CONTINUITY_SHARE = 0.5
        private const val DOMINANT_SHARE = 0.7
        private const val CONTINUITY_DISPLAY_MS = 200L
        private const val VOTE_START_MS = 80L
        private const val VOTE_WINDOW_MS = 450L
        private const val VOTE_CLARITY = 0.8
        private const val VOTE_MAX_CENTS = 100.0
        private const val LOCK_VOTES = 10.0
        private const val OVERRIDE_ANALYSES = 5
        private const val IDLE_BEFORE_LEARNING_MS = 2000L
        private const val ANALYSIS_HISTORY = 16
        private const val PRE_ONSET_MARGIN_MS = 100L

        /** Smallest power of two covering ~80 ms (≥ 2 periods of the 50 Hz lower search limit). */
        fun windowSizeFor(sampleRate: Int): Int {
            val target = (sampleRate * WINDOW_SECONDS).toInt()
            var size = 1024
            while (size < target) size = size shl 1
            return size
        }
    }

    val windowSize: Int = windowSizeFor(sampleRate)
    val hopSize: Int = windowSize / 4

    private val shortMpm = MpmPitchDetector(sampleRate, windowSize)
    private val longMpm = MpmPitchDetector(sampleRate, windowSize * 2)
    private val spectral = HarmonicSpectrumDetector(sampleRate)
    private val learner = BackgroundToneLearner()

    private val bufferSize = max(windowSize * 2, spectral.windowSize)
    private val ring = FloatArray(bufferSize)
    private val frame = FloatArray(windowSize)
    private val longFrame = FloatArray(windowSize * 2)
    private val spectralFrame = FloatArray(spectral.windowSize)
    private var writePosition = 0
    private var totalSamples = 0L
    private var samplesSinceAnalysis = 0
    private var lastMutedSample = Long.MIN_VALUE / 2
    private var hopCount = 0L

    var context: TunerAnalysisContext = context
        private set
    private var highPass1 = Biquad.highPass(sampleRate, context.highPassHz)
    private var highPass2 = Biquad.highPass(sampleRate, context.highPassHz)
    private val lowPass1 = Biquad.lowPass(sampleRate, LOW_PASS_HZ)
    private val lowPass2 = Biquad.lowPass(sampleRate, LOW_PASS_HZ)
    /** Pluck-click detector: energy above ~2 kHz of the raw input (strings themselves are below 1.5 kHz). */
    private val clickHighPass1 = Biquad.highPass(sampleRate, CLICK_HIGH_PASS_HZ)
    private val clickHighPass2 = Biquad.highPass(sampleRate, CLICK_HIGH_PASS_HZ)
    private var clickEnergy = 0.0
    private var notches: List<Biquad> = emptyList()

    private var latestCandidates: List<StringCandidate> = emptyList()
    private var spectralFresh = false
    /** Per-string harmonic scores smoothed over analyses (sympathetic-string-aware, see [arbitratedScores]). */
    private var smoothedScores = DoubleArray(0)
    /** Recent smoothed-score snapshots (time, scores), newest last, for pre-onset baselines. */
    private val scoreHistory = ArrayDeque<Pair<Long, DoubleArray>>()
    private var baselineScores = DoubleArray(0)
    private var seenOnsets = 0
    private var noteVotes = DoubleArray(0)
    /** Votes from evidence about this pluck (everything except the continuity head start). */
    private var evidenceVotes = DoubleArray(0)
    private var noteString: Int? = null
    private var previousNoteString: Int? = null
    private var noteLocked = false
    private var rivalIndex = -1
    private var rivalCount = 0
    private var wasNoteActive = false
    private var lastNoteEndMs = 0L

    init {
        stabilizer.followsLivePitch = context.readingMode == TunerReadingMode.LIVE
    }

    /** Background tones currently being notched out (for diagnostics and UI). */
    val filteredTonesHz: List<Double> get() = learner.tones

    /** Per-hop internals for offline diagnostics (replay tests); null in normal use. */
    var debugListener: ((nowMs: Long, candidates: List<StringCandidate>, baseline: DoubleArray, chosen: Int?, mpm: PitchEstimate?) -> Unit)? = null

    fun setContext(newContext: TunerAnalysisContext) {
        if (newContext == context) return
        // A reading-mode change only affects what is reported, not what is listened for.
        val listeningChanged = newContext.copy(readingMode = context.readingMode) != context
        context = newContext
        stabilizer.followsLivePitch = newContext.readingMode == TunerReadingMode.LIVE
        if (!listeningChanged) return
        highPass1 = Biquad.highPass(sampleRate, newContext.highPassHz)
        highPass2 = Biquad.highPass(sampleRate, newContext.highPassHz)
        rebuildNotches()
        resetStringTracking()
    }

    /**
     * @param muted True while the app plays its own sound; the window is considered contaminated
     *              until enough clean audio has been collected afterwards.
     * @return A new sample when a hop completed during this call, null otherwise.
     */
    fun process(chunk: FloatArray, count: Int = chunk.size, muted: Boolean = false): PitchSample? {
        for (i in 0 until count) {
            val raw = chunk[i].toDouble()
            val click = clickHighPass2.process(clickHighPass1.process(raw))
            clickEnergy += click * click
            var x = highPass2.process(highPass1.process(raw))
            for (notch in notches) x = notch.process(x)
            ring[writePosition] = lowPass2.process(lowPass1.process(x)).toFloat()
            writePosition = (writePosition + 1) % bufferSize
        }
        totalSamples += count
        samplesSinceAnalysis += count
        if (muted) lastMutedSample = totalSamples

        if (totalSamples < windowSize || samplesSinceAnalysis < hopSize) return null
        val transientLevel = sqrt(clickEnergy / samplesSinceAnalysis)
        clickEnergy = 0.0
        samplesSinceAnalysis = 0
        hopCount++

        copyLatest(frame, windowSize)
        var sum = 0.0
        for (x in frame) sum += x.toDouble() * x
        val level = sqrt(sum / windowSize)
        val nowMs = totalSamples * 1000L / sampleRate
        val blocked = !isClean(windowSize)
        val ctx = context

        // 1. Time-domain estimate, octave-resolved and range-checked.
        var mpm: PitchEstimate? = null
        var mpmHz: Double? = null
        if (!blocked) {
            var detector = shortMpm
            mpm = shortMpm.detect(frame)
            if ((mpm == null || mpm.clarity < CONFIDENT_CLARITY) && isClean(longFrame.size)) {
                copyLatest(longFrame, longFrame.size)
                val long = longMpm.detect(longFrame)
                if (long != null && (mpm == null || long.clarity > mpm.clarity)) {
                    mpm = long
                    detector = longMpm
                }
            }
            mpmHz = mpm?.let { estimate ->
                val resolved = OctaveResolver.resolve(estimate.frequencyHz, ctx.targetsHz, detector::supportAt)
                resolved.takeIf { it in ctx.searchRangeHz }
            }
        }

        // 2. Harmonic analysis of each string (guitar tunings only).
        if (!ctx.isChromatic && !blocked && isClean(spectral.windowSize) && hopCount % SPECTRAL_EVERY_HOPS == 0L) {
            copyLatest(spectralFrame, spectral.windowSize)
            latestCandidates = spectral.analyze(spectralFrame, ctx.targetsHz)
            spectralFresh = true
            if (smoothedScores.size != ctx.targetsHz.size) smoothedScores = DoubleArray(ctx.targetsHz.size)
            val arbitrated = arbitratedScores(latestCandidates)
            for (i in smoothedScores.indices) smoothedScores[i] = 0.5 * smoothedScores[i] + 0.5 * (arbitrated[i] ?: 0.0)
            scoreHistory.addLast(nowMs to smoothedScores.copyOf())
            while (scoreHistory.size > ANALYSIS_HISTORY) scoreHistory.removeFirst()
        }

        // 3. Which string is this note? Decided by a vote over the start of each note, then locked.
        if (stabilizer.onsetCount != seenOnsets) {
            seenOnsets = stabilizer.onsetCount
            // What was ringing *before* the pluck: the latest scores from well before the onset
            // (the onset itself is detected up to one window late).
            baselineScores = scoreHistory.lastOrNull { it.first <= stabilizer.lastOnsetMs - PRE_ONSET_MARGIN_MS }
                ?.second?.copyOf() ?: DoubleArray(ctx.targetsHz.size)
            noteVotes = DoubleArray(ctx.targetsHz.size)
            evidenceVotes = DoubleArray(ctx.targetsHz.size)
            // Tuning means re-plucking the same string while turning its peg: if the previous
            // note's string is still one of the strongest, it starts with a head start.
            val previous = noteString
            previousNoteString = previous
            val strongest = smoothedScores.maxOrNull() ?: 0.0
            if (previous != null && previous < noteVotes.size && strongest > 0.0 &&
                smoothedScores.getOrElse(previous) { 0.0 } >= CONTINUITY_SHARE * strongest
            ) {
                noteVotes[previous] += CONTINUITY_VOTES
            }
            noteString = null
            noteLocked = false
            rivalCount = 0
        }
        val chosen = ctx.lockedIndex ?: identifyString(nowMs, mpmHz, mpm?.clarity ?: 0.0)
        spectralFresh = false

        // 4. Fusion.
        val spec = latestCandidates.firstOrNull { it.index == chosen }
        val mpmClarity = mpm?.clarity ?: 0.0
        var hint: Int? = null
        val estimate: PitchEstimate? = when {
            ctx.isChromatic ->
                mpmHz?.let { PitchEstimate(it, mpmClarity) }
            // The harmonic estimate (lower partials) is the reference. MPM measures the whole
            // waveform period, which stiff-string upper partials pull sharp, so it only refines the
            // value when it is clean and agrees within a few cents (plain strings, pure tones).
            spec != null && mpmHz != null && mpmClarity >= ASSISTED_CLARITY &&
                abs(PitchMath.cents(mpmHz, spec.frequencyHz)) <= AGREE_CENTS -> {
                hint = chosen
                PitchEstimate(mpmHz, mpmClarity)
            }
            spec != null -> {
                hint = chosen
                PitchEstimate(spec.frequencyHz, SPECTRAL_CONFIDENCE)
            }
            // Without harmonic confirmation, a clear pitch is only shown when it belongs to this
            // note's identified string (a different string still ringing must not be displayed
            // against it, and nothing is shown while a fresh pluck is still being identified).
            mpmHz != null && mpmClarity >= SOLO_CLARITY && soloAllowed(mpmHz, chosen, ctx.targetsHz) ->
                PitchEstimate(mpmHz, mpmClarity)
            else -> null
        }

        // 5. Learn steady background tones while idle.
        val idle = !stabilizer.isNoteActive &&
            (stabilizer.onsetCount == 0 || nowMs - lastNoteEndMs >= IDLE_BEFORE_LEARNING_MS)
        if (!blocked) {
            learner.observe(mpm, nowMs, idle, ctx.targetsHz)?.let { rebuildNotches() }
        }

        debugListener?.invoke(nowMs, latestCandidates, smoothedScores, chosen, mpm)
        val sample = stabilizer.process(estimate, level, nowMs, blocked, hint, transientLevel)
        if (wasNoteActive && !stabilizer.isNoteActive) lastNoteEndMs = nowMs
        wasNoteActive = stabilizer.isNoteActive
        return sample
    }

    fun reset() {
        highPass1.reset(); highPass2.reset(); lowPass1.reset(); lowPass2.reset()
        clickHighPass1.reset(); clickHighPass2.reset()
        clickEnergy = 0.0
        notches.forEach { it.reset() }
        ring.fill(0f)
        writePosition = 0
        totalSamples = 0
        samplesSinceAnalysis = 0
        lastMutedSample = Long.MIN_VALUE / 2
        hopCount = 0
        resetStringTracking()
        seenOnsets = 0
        wasNoteActive = false
        lastNoteEndMs = 0
        stabilizer.reset()
    }

    /**
     * Identifies the plucked string. During the first [VOTE_WINDOW_MS] of a note the plucked
     * string dominates; afterwards sympathetic strings can take over (on a real unplugged electric,
     * plucking the high E makes the open low E ring louder than the high E within ~400 ms). So:
     *
     * - **Vote** while the note is young: a confident time-domain pitch votes for its nearest
     *   string (weight 2) when the harmonic analysis agrees that string is dominant; each harmonic
     *   analysis votes for the strongest string whose evidence *rose* since the pluck (weight 1),
     *   so strings that were already ringing can't win; the previous note's string gets a small
     *   head start when it is still among the strongest (re-plucks while turning the peg).
     * - **Lock** the leader for the rest of the note.
     * - **Override** only if another string's harmonic score stays more than double the locked
     *   one's for [OVERRIDE_ANALYSES] analyses (e.g. a second pluck without a detectable onset).
     */
    private fun identifyString(nowMs: Long, mpmHz: Double?, mpmClarity: Double): Int? {
        val targets = context.targetsHz
        if (targets.isEmpty() || noteVotes.size != targets.size) return noteString
        if (!noteLocked) {
            if (stabilizer.isNoteActive && nowMs >= stabilizer.lastOnsetMs + VOTE_START_MS) {
                if (mpmHz != null && mpmClarity >= VOTE_CLARITY) {
                    val nearest = targets.indices.minBy { abs(PitchMath.cents(mpmHz, targets[it])) }
                    // A sympathetically ringing string can dominate the waveform; its vote only
                    // counts if the harmonic analysis also sees it as a dominant string.
                    // A string that was already ringing and is now decaying (the previous note)
                    // must not collect votes for the new pluck: its evidence has to be rising.
                    val strongest = smoothedScores.maxOrNull() ?: 0.0
                    val harmonicAgreement = strongest <= 0.0 ||
                        (smoothedScores.getOrElse(nearest) { 0.0 } >= DOMINANT_SHARE * strongest && isRising(nearest))
                    if (abs(PitchMath.cents(mpmHz, targets[nearest])) <= VOTE_MAX_CENTS && harmonicAgreement) {
                        noteVotes[nearest] += 2.0
                        evidenceVotes[nearest] += 2.0
                    }
                }
                if (spectralFresh) risingStrongest()?.let {
                    noteVotes[it] += 1.0
                    evidenceVotes[it] += 1.0
                }
            }
            val leader = noteVotes.indices.maxBy { noteVotes[it] }.takeIf { noteVotes[it] > 0.0 }
            // The continuity head start may decide, but early in the note only evidence about this
            // pluck can put a string on screen (otherwise the previous string lingers after a change).
            val shown = leader?.takeIf { evidenceVotes[it] > 0.0 || nowMs >= stabilizer.lastOnsetMs + CONTINUITY_DISPLAY_MS }
            if (shown != null) {
                if (shown != noteString && previousNoteString != null && shown != previousNoteString) {
                    // A different string was plucked: the previous string's held value is stale.
                    stabilizer.dropHeldValue()
                }
                noteString = shown
                if (nowMs >= stabilizer.lastOnsetMs + VOTE_WINDOW_MS || noteVotes[shown] >= LOCK_VOTES) noteLocked = true
            }
        } else if (spectralFresh) {
            val locked = noteString
            val strongest = smoothedScores.indices.maxByOrNull { smoothedScores[it] }
            if (locked != null && strongest != null && strongest != locked &&
                smoothedScores[strongest] > 2.0 * smoothedScores.getOrElse(locked) { 0.0 } + MIN_NOVELTY
            ) {
                if (rivalIndex == strongest) rivalCount++ else { rivalIndex = strongest; rivalCount = 1 }
                if (rivalCount >= OVERRIDE_ANALYSES) {
                    noteString = strongest
                    rivalCount = 0
                }
            } else {
                rivalCount = 0
            }
        }
        return noteString
    }

    /**
     * A clear MPM pitch without harmonic confirmation may be shown when it belongs to the note's
     * identified string, or — before identification — when it isn't the previous note's string
     * still ringing (that one is stale the moment another string is plucked).
     */
    private fun soloAllowed(mpmHz: Double, chosen: Int?, targets: List<Double>): Boolean {
        if (targets.isEmpty()) return true
        val nearest = targets.indices.minBy { abs(PitchMath.cents(mpmHz, targets[it])) }
        return if (chosen != null) abs(PitchMath.cents(mpmHz, targets[chosen])) <= VOTE_MAX_CENTS
        else !(stabilizer.isNoteActive && nearest == previousNoteString)
    }

    /** Strongest string whose smoothed evidence rose clearly since the pluck, or null. */
    private fun risingStrongest(): Int? {
        var best: Int? = null
        for (i in smoothedScores.indices) {
            if (isRising(i) && (best == null || smoothedScores[i] > smoothedScores[best])) best = i
        }
        return best
    }

    /**
     * Whether a string's harmonic evidence rose since the pluck. Re-plucking a string that still
     * rings only adds a fraction of its energy, so the rise is judged in absolute terms with a
     * modest relative floor.
     */
    private fun isRising(index: Int): Boolean {
        val base = baselineScores.getOrElse(index) { 0.0 }
        return smoothedScores.getOrElse(index) { 0.0 } - base >= maxOf(MIN_NOVELTY, RISE_FRACTION * base)
    }

    private fun resetStringTracking() {
        latestCandidates = emptyList()
        spectralFresh = false
        smoothedScores = DoubleArray(0)
        scoreHistory.clear()
        baselineScores = DoubleArray(0)
        noteVotes = DoubleArray(0)
        evidenceVotes = DoubleArray(0)
        noteString = null
        previousNoteString = null
        noteLocked = false
        rivalCount = 0
    }

    /**
     * Scores per string, where a lower string loses the harmonics it shares with a higher string
     * that is also present at an integer multiple (low E's 3rd/4th harmonics *are* the B and high E
     * strings, and the open low E rings in sympathy when they're plucked).
     */
    private fun arbitratedScores(candidates: List<StringCandidate>): Map<Int, Double> {
        val targets = context.targetsHz
        val result = HashMap<Int, Double>(candidates.size)
        for (low in candidates) {
            var score = low.score
            for (high in candidates) {
                if (high.index == low.index || high.index !in targets.indices || low.index !in targets.indices) continue
                val ratio = targets[high.index] / targets[low.index]
                val m = Math.round(ratio).toInt()
                if (m >= 2 && abs(PitchMath.cents(ratio, m.toDouble())) <= 30.0) {
                    score = minOf(score, low.scoreExcludingMultiplesOf(m))
                }
            }
            result[low.index] = score
        }
        return result
    }

    private fun rebuildNotches() {
        notches = learner.tones.flatMap { tone ->
            learner.notchFrequencies(tone, context.targetsHz, LOW_PASS_HZ).map { Biquad.notch(sampleRate, it, NOTCH_Q) }
        }
    }

    /** True when the last [length] samples are all real audio, untouched by our own sounds. */
    private fun isClean(length: Int) = totalSamples >= length && totalSamples - lastMutedSample >= length

    private fun copyLatest(target: FloatArray, length: Int) {
        var start = writePosition - length
        if (start < 0) start += bufferSize
        val firstPart = minOf(length, bufferSize - start)
        System.arraycopy(ring, start, target, 0, firstPart)
        if (firstPart < length) System.arraycopy(ring, 0, target, firstPart, length - firstPart)
    }
}
