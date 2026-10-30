package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Stabilized pitch output of the tuner pipeline for one analysis hop.
 *
 * @param frequencyHz Stable pitch, or null when nothing reliable is ringing.
 * @param clarity Confidence of the latest raw estimate (0 when unavailable).
 * @param level RMS level of the analysis window in [0, 1].
 * @param isHolding True when [frequencyHz] is a frozen previous value kept while the note decays.
 * @param isSignalPresent True when the level is clearly above the room's noise floor.
 * @param timestampMs Stream time of this sample, derived from the sample count.
 * @param isInputSilenced True when the OS is silencing our capture because another app took the mic.
 * @param isSettled In steady reading mode, false during the first ~250 ms of a note, while the
 *        pluck's pitch glide (strings start sharp) is still decaying; in-tune decisions are only
 *        made on settled readings. Always true in live mode once the attack has passed.
 * @param stringHint Index of the string the pipeline identified (harmonic analysis of the pluck),
 *        or null when it only knows the pitch.
 * @param isNoteActive True between a pluck onset and the note's release.
 */
data class PitchSample(
    val frequencyHz: Double?,
    val clarity: Double,
    val level: Double,
    val isHolding: Boolean,
    val isSignalPresent: Boolean,
    val timestampMs: Long,
    val isInputSilenced: Boolean = false,
    val isSettled: Boolean = true,
    val stringHint: Int? = null,
    val isNoteActive: Boolean = false
)

/**
 * Turns per-hop pitch estimates into a steady, trustworthy reading, organised around *notes*:
 *
 * - **Onset-anchored notes.** Nothing is shown until a pluck onset: a level jump of ≥ [onsetRatio]
 *   over the recent minimum, at least [gateRatio] above the noise floor. Steady background sound
 *   (hum, fridge, fan) never creates an onset, so it can never be displayed as a note. The note
 *   lasts until the level stays near the floor for [releaseMs].
 * - **Robust noise floor** via [NoiseFloorEstimator] (minimum statistics, start-up warm-up,
 *   frozen while a note rings).
 * - **Attack handling.** The first [attackMs] are skipped (pick click).
 * - **Two reading modes** ([followsLivePitch]). Plucked strings start sharp and sink as they ring.
 *   - *Live*: the reading follows the note from the attack on, and in-tune decisions are made on
 *     it straight away, the way most tuners behave.
 *   - *Steady*: until [settleMs] the reading is flagged unsettled (no in-tune decisions); from
 *     [anchorMs] on it stays at the note's early-sustain value while the string sinks as it fades.
 *     Turning the peg (any sharp move, or a large flat one) releases it so the display follows.
 * - **Cluster estimate.** The output is the mean of the recent estimates within [clusterCents] of
 *   their median (less jitter than a plain median); a jump of more than [outlierCents] needs
 *   [confirmFrames] agreeing frames.
 * - **Decay hold** keeps the last value for [holdMs] so the display doesn't flicker.
 */
class PitchStabilizer(
    private val minConfidence: Double = 0.80,
    private val gateRatio: Double = 2.8,
    private val releaseRatio: Double = 1.6,
    private val absoluteGate: Double = 0.0001,
    private val onsetRatio: Double = 1.6,
    private val onsetLookbackHops: Int = 4,
    /** A pluck's pick click and string bloom are separate level jumps; count them as one onset. */
    private val minOnsetGapMs: Long = 300,
    /**
     * A pluck starts with a broadband click: the high-frequency level must jump by this ratio too.
     * Slow swells from strings beating against each other (sympathetic resonance) don't, and must
     * not restart string identification.
     */
    private val transientRatio: Double = 2.0,
    /** A level jump this large counts as a pluck even without a detectable click (soft fingers). */
    private val strongOnsetRatio: Double = 3.0,
    private val attackMs: Long = 80,
    private val settleMs: Long = 250,
    /**
     * The note's reading is anchored to its early sustain — the median over settle … [anchorMs]
     * after the pluck:
     * a plucked string — especially a heavy wound one — sinks in pitch as it rings (measured on a
     * real low E: 15–20¢ over 2 s), so following the whole decay makes the needle wander and
     * disagree with conventional tuners, which read the note as it sounds when played.
     */
    private val anchorMs: Long = 850,
    /**
     * Anchor readings are collected from here on: each reading looks back over its analysis window
     * (up to ~340 ms), so earlier ones still contain the sharp attack.
     */
    private val anchorStartMs: Long = 350,
    /** The anchor needs this many settled readings; if a mute interrupted the window, it waits for more. */
    private val minAnchorSamples: Int = 10,
    /** Turning the peg sharp releases the anchor (natural decay only ever drifts flat). */
    private val releaseSharpCents: Double = 4.0,
    /** Drifting flat further than natural decay does releases the anchor too (peg turned down). */
    private val releaseFlatCents: Double = 20.0,
    private val releaseMs: Long = 300,
    /** A "note" with no usable pitch for this long is noise (a fan switching on), not a string. */
    private val noPitchReleaseMs: Long = 1500,
    private val holdMs: Long = 1000,
    private val historySize: Int = 9,
    private val clusterCents: Double = 8.0,
    private val outlierCents: Double = 50.0,
    private val confirmFrames: Int = 3,
    private val floorEstimator: NoiseFloorEstimator = NoiseFloorEstimator()
) {
    private val recentLevels = DoubleArray(onsetLookbackHops)
    private val recentTransients = DoubleArray(onsetLookbackHops)
    private var recentCount = 0
    private var recentIndex = 0

    var isNoteActive = false
        private set
    /** Increments on every pluck onset (lets the tracker re-identify the string per note). */
    var onsetCount = 0
        private set
    /** Time of the latest onset. */
    val lastOnsetMs: Long get() = noteStartMs
    private var noteStartMs = 0L
    /** Anchored reading (log2 Hz) for the current note, or NaN while still measuring / released. */
    private var anchorLog = Double.NaN
    private var anchorReleased = false
    /** Settled readings (log2 Hz) collected during the anchor window. */
    private val anchorSamples = ArrayList<Double>()
    private var lastAboveMs = 0L
    private var lastPitchMs = 0L
    private var settled = false

    /** True: report the live pitch from the attack on. False: report the steady early-sustain value. */
    var followsLivePitch = true

    private val history = ArrayDeque<Double>()   // log2(frequency)
    private val pending = ArrayDeque<Double>()
    private val sortBuffer = DoubleArray(historySize)

    private var lastOutputHz: Double? = null
    private var lastValidMs = 0L
    private var lastHint: Int? = null

    val noiseFloor: Double? get() = floorEstimator.floor

    /** Level a pluck must exceed to start a note (infinite while the floor is still warming up). */
    val gateThreshold: Double
        get() = floorEstimator.floor?.let { max(absoluteGate, it * gateRatio) } ?: Double.MAX_VALUE

    /**
     * @param estimate Fused detector output for this hop (null if nothing trustworthy was found).
     * @param level RMS level of the analysed window.
     * @param blocked True while analysis is suspended (e.g. the app itself is playing a sound).
     * @param stringHint String identified by the harmonic analysis, passed through to the sample.
     * @param transientLevel RMS of the input above ~2 kHz for this hop (pluck clicks); negative when
     *        unavailable, in which case only the level jump is used.
     */
    fun process(
        estimate: PitchEstimate?,
        level: Double,
        nowMs: Long,
        blocked: Boolean = false,
        stringHint: Int? = null,
        transientLevel: Double = -1.0
    ): PitchSample {
        if (blocked) return holdOrSilence(nowMs, level, clarity = 0.0, signalPresent = false)

        val floor = floorEstimator.update(level, nowMs, frozen = isNoteActive)
        val signalPresent = level > gateThreshold
        var recentMin = level
        var recentTransientMin = transientLevel
        for (i in 0 until recentCount) {
            recentMin = minOf(recentMin, recentLevels[i])
            recentTransientMin = minOf(recentTransientMin, recentTransients[i])
        }
        recentLevels[recentIndex] = level
        recentTransients[recentIndex] = transientLevel
        recentIndex = (recentIndex + 1) % recentLevels.size
        recentCount = minOf(recentCount + 1, recentLevels.size)

        val levelJump = level >= recentMin * onsetRatio
        val hasClick = transientLevel < 0 || transientLevel >= recentTransientMin * transientRatio
        val strongJump = level >= recentMin * strongOnsetRatio
        val gapOk = onsetCount == 0 || nowMs - noteStartMs >= minOnsetGapMs
        val clearPitchPresent = !isNoteActive && estimate != null && estimate.clarity >= minConfidence && signalPresent
        if ((signalPresent && levelJump && (hasClick || strongJump) && gapOk) || clearPitchPresent) {
            isNoteActive = true
            onsetCount++
            noteStartMs = nowMs
            anchorLog = Double.NaN
            anchorReleased = false
            anchorSamples.clear()
            lastAboveMs = nowMs
            lastPitchMs = nowMs
            settled = false
            history.clear()
            pending.clear()
        }
        val clarity = estimate?.clarity ?: 0.0
        if (isNoteActive && floor != null) {
            if (level > max(absoluteGate, floor * releaseRatio)) lastAboveMs = nowMs
            if (estimate != null && clarity >= minConfidence) lastPitchMs = nowMs
            if (nowMs - lastAboveMs > releaseMs || nowMs - lastPitchMs > noPitchReleaseMs) isNoteActive = false
        }

        if (!isNoteActive || nowMs < noteStartMs + attackMs || estimate == null || clarity < minConfidence) {
            return holdOrSilence(nowMs, level, clarity, signalPresent)
        }

        if (!settled && (followsLivePitch || nowMs - noteStartMs >= settleMs)) {
            // Steady mode: drop the glide frames so the settled reading isn't biased sharp.
            settled = true
            history.clear()
            pending.clear()
        }

        val logF = log2(estimate.frequencyHz)
        if (history.size >= confirmFrames && abs(logF - median()) * 1200.0 > outlierCents) {
            if (pending.isNotEmpty() && abs(logF - pending.last()) * 1200.0 > outlierCents) pending.clear()
            pending.addLast(logF)
            if (pending.size < confirmFrames) {
                // Still a suspected outlier: keep showing the established value.
                return PitchSample(lastOutputHz, clarity, level, isHolding = false, isSignalPresent = signalPresent,
                    timestampMs = nowMs, isSettled = settled, stringHint = lastHint, isNoteActive = true)
            }
            history.clear()
            history.addAll(pending)
            pending.clear()
        } else {
            pending.clear()
            history.addLast(logF)
            while (history.size > historySize) history.removeFirst()
        }

        val live = clusterMean()
        if (!followsLivePitch && settled && !anchorReleased && anchorLog.isNaN()) {
            if (nowMs - noteStartMs < anchorMs || anchorSamples.size < minAnchorSamples) {
                if (nowMs - noteStartMs >= anchorStartMs) anchorSamples.add(live)
            } else {
                anchorSamples.sort()
                anchorLog = anchorSamples[anchorSamples.size / 2]
            }
        }
        if (!anchorLog.isNaN()) {
            val drift = (live - anchorLog) * 1200.0
            if (drift > releaseSharpCents || drift < -releaseFlatCents) {
                anchorLog = Double.NaN
                anchorReleased = true
            }
        }
        val output = 2.0.pow(if (anchorLog.isNaN() || followsLivePitch) live else anchorLog)
        lastOutputHz = output
        lastValidMs = nowMs
        lastHint = stringHint
        return PitchSample(output, clarity, level, isHolding = false, isSignalPresent = signalPresent,
            timestampMs = nowMs, isSettled = settled, stringHint = stringHint, isNoteActive = true)
    }

    /** Forget the displayed value (a different string was just plucked; holding the old one would mislead). */
    fun dropHeldValue() {
        anchorLog = Double.NaN
        anchorSamples.clear()
        lastOutputHz = null
        lastHint = null
        history.clear()
        pending.clear()
    }

    fun reset() {
        floorEstimator.reset()
        recentCount = 0
        recentIndex = 0
        isNoteActive = false
        settled = false
        history.clear()
        pending.clear()
        lastOutputHz = null
        lastValidMs = 0L
        lastHint = null
    }

    private fun holdOrSilence(nowMs: Long, level: Double, clarity: Double, signalPresent: Boolean): PitchSample {
        val held = lastOutputHz
        if (held != null && nowMs - lastValidMs <= holdMs) {
            return PitchSample(held, clarity, level, isHolding = true, isSignalPresent = signalPresent,
                timestampMs = nowMs, isSettled = settled, stringHint = lastHint, isNoteActive = isNoteActive)
        }
        lastOutputHz = null
        lastHint = null
        if (!isNoteActive) {
            history.clear()
            pending.clear()
        }
        return PitchSample(null, clarity, level, isHolding = false, isSignalPresent = signalPresent,
            timestampMs = nowMs, isNoteActive = isNoteActive)
    }

    private fun median(): Double {
        val n = history.size
        for (i in 0 until n) sortBuffer[i] = history[i]
        sortBuffer.sort(0, n)
        return if (n % 2 == 1) sortBuffer[n / 2] else (sortBuffer[n / 2 - 1] + sortBuffer[n / 2]) / 2.0
    }

    private fun clusterMean(): Double {
        val center = median()
        var sum = 0.0
        var count = 0
        for (value in history) {
            if (abs(value - center) * 1200.0 <= clusterCents) {
                sum += value
                count++
            }
        }
        return if (count == 0) center else sum / count
    }

    private fun log2(x: Double) = ln(x) / LN2

    private companion object {
        val LN2 = ln(2.0)
    }
}
