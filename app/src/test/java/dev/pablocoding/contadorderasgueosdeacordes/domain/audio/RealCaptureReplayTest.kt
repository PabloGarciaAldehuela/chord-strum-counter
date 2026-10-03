package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Replays real recordings from a Pixel 9 Pro XL through the full pipeline.
 *
 * The recordings contain room speech, so they are **never committed**: they live in the
 * git-ignored `docs/tuner-investigation/captures/` and these tests are skipped when absent.
 */
class RealCaptureReplayTest {

    private val standard = TuningLibrary.standard

    /**
     * Unplugged electric guitar, UNPROCESSED source. The tuner reports each note's early sustain
     * (median over 0.25–0.9 s after the pluck; plucked strings sink in pitch as they ring), so the
     * independent truth is measured over the same window: power-weighted harmonics h1–h6 standing
     * ≥ 15 dB above the local noise in one 0.65 s FFT. [required] = false when that window holds no
     * measurable harmonic (the A at 69.6 s only starts sounding about a second after its click).
     */
    private data class Pluck(val timeS: Double, val stringNumber: Int, val truthCents: Double, val required: Boolean = true)

    private val take2Plucks = listOf(
        // 15.27 s: the B pluck (the in-tune chime that followed at 16.2 s is muted on the device).
        Pluck(15.27, 2, -3.0), Pluck(17.74, 1, -4.4), Pluck(34.82, 5, -2.6), Pluck(45.00, 2, -3.9),
        Pluck(50.18, 1, -4.9), Pluck(58.12, 2, 0.0, required = false), Pluck(62.54, 6, -19.0),
        Pluck(69.62, 5, 0.0, required = false)
    )

    private fun report(): Pair<Int, List<String>> {
        val file = TunerReplay.capture("take2/capture_1791027985914.wav")
        assumeTrue("real capture not available", file != null)
        val (sampleRate, audio) = TunerReplay.readFloatWav(file!!)
        val frames = TunerReplay.run(audio, sampleRate, standard)
        var correct = 0
        val rows = take2Plucks.map { pluck ->
            val from = (pluck.timeS * 1000).toLong()
            val window = frames.filter { it.timeMs in from..from + 1500 && it.reading.hasPitch && !it.reading.isHolding }
            if (window.isEmpty()) return@map "s${pluck.stringNumber}: nothing${if (pluck.required) "" else " (not required)"}"
            val settled = window.filter { it.sample.isSettled }.ifEmpty { window }
            val cents = settled.map { it.reading.cents!! }.sorted()
            val median = cents[cents.size / 2]
            val shownString = settled.groupingBy { it.reading.targetStringIndex }.eachCount().maxBy { it.value }.key
            val shownNumber = shownString?.let { standard.stringNumber(it) }
            val ok = shownNumber == pluck.stringNumber && abs(median - pluck.truthCents) <= 2.5 && window.size >= 10
            if (ok && pluck.required) correct++
            "s${pluck.stringNumber}: shown s$shownNumber ${"%+.1f".format(median)}¢ (truth ${"%+.1f".format(pluck.truthCents)}, ${window.size} frames)${if (ok) " ✓" else " ✗"}"
        }
        return correct to rows
    }

    @Test
    fun `real unplugged electric plucks are identified and measured`() {
        val (correct, rows) = report()
        rows.forEach(::println)
        val required = take2Plucks.count { it.required }
        println("take2: $correct/$required required plucks correct")
        assertTrue("Only $correct/$required plucks correct:\n${rows.joinToString("\n")}", correct >= required)
    }

    @Test
    fun `reference tones from every audio source read within a third of a cent`() {
        for ((name, onsetS) in listOf(
            "calib/capture_1791028380954.wav" to 4.6,   // UNPROCESSED
            "calib/capture_1791028433132.wav" to 3.3,   // MIC
            "calib/capture_1791028466698.wav" to 3.8    // VOICE_RECOGNITION
        )) {
            val file = TunerReplay.capture(name)
            assumeTrue("calibration capture not available", file != null)
            val (sampleRate, audio) = TunerReplay.readFloatWav(file!!)
            val frames = TunerReplay.run(audio, sampleRate, standard)
            for ((expected, from) in listOf(110.0 to onsetS + 0.5, 329.6276 to onsetS + 5.0)) {
                val window = frames.filter { it.timeMs in (from * 1000).toLong()..((from + 3) * 1000).toLong() && it.reading.hasPitch }
                assertTrue("$name: no readings for $expected Hz", window.size > 50)
                val freqs = window.map { it.reading.frequencyHz!! }.sorted()
                val error = PitchMath.cents(freqs[freqs.size / 2], expected)
                println("$name $expected Hz: ${"%+.3f".format(error)}¢ over ${window.size} frames")
                assertTrue("$name $expected Hz off by $error¢", abs(error) <= 0.3)
            }
        }
    }

    @Test
    fun `string changes follow the plucks with no lingering wrong string`() {
        // Session 3 of take3: A, D, G, B, high E, low E plucked in turn (each re-plucked once),
        // with the previous strings still ringing. Pluck times are the pick clicks in the audio.
        val file = TunerReplay.capture("take3/capture_1791037846847.wav")
        assumeTrue("real capture not available", file != null)
        val (sampleRate, audio) = TunerReplay.readFloatWav(file!!)
        val frames = TunerReplay.run(audio, sampleRate, standard)
        val plucks = listOf(7.87 to 5, 11.03 to 4, 13.16 to 3, 15.21 to 2, 17.39 to 1, 20.01 to 6)
        val nextPluck = listOf(10.09, 11.99, 14.31, 16.64, 18.47, 22.0)
        for ((i, pluck) in plucks.withIndex()) {
            val (t, stringNumber) = pluck
            val window = frames.filter { it.timeMs / 1000.0 in t..nextPluck[i] && it.reading.hasPitch && !it.reading.isHolding }
            val shown = window.filter { it.timeMs / 1000.0 > t + 0.6 }.groupingBy { it.reading.targetStringIndex }.eachCount().maxBy { it.value }.key
            assertTrue("pluck at $t: shown string ${shown?.let { standard.stringNumber(it) }}, expected $stringNumber",
                shown != null && standard.stringNumber(shown) == stringNumber)
            val firstCorrect = window.first { it.reading.targetStringIndex == shown }
            val liveWrong = window.count { it.timeMs < firstCorrect.timeMs && it.reading.targetStringIndex != shown }
            println("take3 pluck $t → string $stringNumber after ${firstCorrect.timeMs - (t * 1000).toLong()} ms, $liveWrong live wrong frames")
            assertTrue("pluck at $t: $liveWrong live frames of a wrong string", liveWrong <= 4)
            assertTrue("pluck at $t: identified after ${firstCorrect.timeMs - (t * 1000).toLong()} ms", firstCorrect.timeMs - (t * 1000).toLong() <= 500)
        }
    }
}
