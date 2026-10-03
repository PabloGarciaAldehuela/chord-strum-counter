package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Test helper: runs audio through the full tuner pipeline (tracker + evaluator) like the app does. */
object TunerReplay {

    data class Frame(val timeMs: Long, val reading: TunerReading, val sample: PitchSample)

    fun run(
        audio: FloatArray,
        sampleRate: Int,
        tuning: Tuning,
        referenceHz: Double = 440.0,
        lockedIndex: Int? = null,
        toleranceCents: Double = 3.0,
        muteAppChime: Boolean = true,
        // The accuracy suites check the steady early-sustain value; live mode has its own tests.
        readingMode: TunerReadingMode = TunerReadingMode.STEADY
    ): List<Frame> {
        val muted = if (muteAppChime) chimeMuteMask(audio, sampleRate) else null
        val context = TunerAnalysisContext(tuning.strings.map { PitchMath.midiToFrequency(it, referenceHz) }, lockedIndex, readingMode)
        val tracker = PitchTracker(sampleRate, context = context)
        val evaluator = TuningEvaluator()
        val chunk = FloatArray(tracker.hopSize)
        val frames = ArrayList<Frame>()
        var position = 0
        while (position + chunk.size <= audio.size) {
            System.arraycopy(audio, position, chunk, 0, chunk.size)
            val isMuted = muted?.let { it[position / chunk.size] } ?: false
            position += chunk.size
            val sample = tracker.process(chunk, muted = isMuted) ?: continue
            frames.add(Frame(sample.timestampMs, evaluator.evaluate(sample, tuning, referenceHz, lockedIndex, toleranceCents), sample))
        }
        return frames
    }

    /**
     * On the device, detection is muted while the app's own "in tune" chime (2093 + 3136 Hz,
     * 380 ms) plays plus a 250 ms echo tail. Recordings contain that chime, so replays must mute
     * the same spans: blocks where the 2093 Hz component dominates start a 630 ms mute.
     */
    private fun chimeMuteMask(audio: FloatArray, sampleRate: Int, block: Int = PitchTracker.windowSizeFor(sampleRate) / 4): BooleanArray {
        val blocks = audio.size / block
        val mask = BooleanArray(blocks + 1)
        val muteBlocks = (0.63 * sampleRate / block).toInt() + 1
        val coefficient = 2 * kotlin.math.cos(2 * Math.PI * 2093.0 / sampleRate)
        var b = 0
        while (b < blocks) {
            var s1 = 0.0; var s2 = 0.0; var energy = 0.0
            for (i in b * block until (b + 1) * block) {
                val x = audio[i].toDouble()
                val s0 = x + coefficient * s1 - s2
                s2 = s1; s1 = s0
                energy += x * x
            }
            val tonePower = (s1 * s1 + s2 * s2 - coefficient * s1 * s2) * 2 / (block.toDouble() * block)
            val blockPower = energy / block
            if (blockPower > 1e-9 && tonePower > 0.25 * blockPower) {
                for (k in b until minOf(blocks + 1, b + muteBlocks)) mask[k] = true
                b += muteBlocks
            } else b++
        }
        return mask
    }

    /** Reads a mono 32-bit float WAV as written by the app's diagnostics capture. */
    fun readFloatWav(file: File): Pair<Int, FloatArray> {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sampleRate = buffer.getInt(24).takeIf { it > 0 } ?: 48_000
        val count = (bytes.size - 44) / 4
        buffer.position(44)
        return sampleRate to FloatArray(count) { buffer.float }
    }

    /** Local, git-ignored real-device captures (absent on CI and other machines). */
    fun capture(relativePath: String): File? {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "docs/tuner-investigation/captures/$relativePath")
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        return null
    }
}
