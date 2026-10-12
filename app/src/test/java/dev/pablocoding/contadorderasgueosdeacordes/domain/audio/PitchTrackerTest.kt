package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * End-to-end tests of the streaming pipeline (filters → MPM → stabilizer) on synthetic plucks.
 */
class PitchTrackerTest {

    private val sampleRate = 48_000

    /** Streams [signal] through the tracker in hop-sized chunks and returns every output sample. */
    private fun stream(tracker: PitchTracker, signal: FloatArray, muted: (Int) -> Boolean = { false }): List<PitchSample> {
        val out = mutableListOf<PitchSample>()
        val chunk = FloatArray(tracker.hopSize)
        var position = 0
        while (position + chunk.size <= signal.size) {
            System.arraycopy(signal, position, chunk, 0, chunk.size)
            tracker.process(chunk, chunk.size, muted(position))?.let(out::add)
            position += chunk.size
        }
        return out
    }

    /** 0.5 s of quiet room noise followed by a decaying pluck. */
    private fun pluck(frequencyHz: Double, seconds: Double = 2.0, snrDb: Double? = null, fundamentalGainDb: Double = -10.0): FloatArray {
        val silence = SyntheticSignals.noise(sampleRate / 2, amplitude = 0.0005)
        var note = SyntheticSignals.string(frequencyHz, sampleRate, (sampleRate * seconds).toInt(), fundamentalGainDb = fundamentalGainDb, decayPerSecond = 1.5)
        if (snrDb != null) note = SyntheticSignals.withNoise(note, snrDb)
        return silence + note
    }

    @Test
    fun `window and hop sizes cover two periods of 50 Hz`() {
        assertEquals(4096, PitchTracker.windowSizeFor(48_000))
        assertEquals(4096, PitchTracker.windowSizeFor(44_100))
        assertEquals(2048, PitchTracker.windowSizeFor(16_000))
        assertEquals(1024, PitchTracker(48_000).hopSize)
    }

    @Test
    fun `every open string converges within one cent`() {
        for (f in listOf(55.0, 65.41, 73.42, 82.41, 110.0, 146.83, 196.0, 246.94, 329.63)) {
            val samples = stream(PitchTracker(sampleRate), pluck(f))
            val stable = samples.filter { it.frequencyHz != null && !it.isHolding }
            assertTrue("No stable readings for $f Hz", stable.size > 20)
            stable.drop(3).forEach {
                val error = SyntheticSignals.cents(it.frequencyHz!!, f)
                assertTrue("$f Hz reading off by $error cents", abs(error) <= 1.0)
            }
        }
    }

    @Test
    fun `noisy room at 10 dB SNR still converges within one and a half cents`() {
        for (f in listOf(73.42, 82.41, 110.0, 246.94)) {
            val samples = stream(PitchTracker(sampleRate), pluck(f, snrDb = 10.0))
            val stable = samples.filter { it.frequencyHz != null && !it.isHolding }.drop(3)
            assertTrue("No stable readings for noisy $f Hz", stable.isNotEmpty())
            stable.forEach {
                val error = SyntheticSignals.cents(it.frequencyHz!!, f)
                assertTrue("noisy $f Hz off by $error cents", abs(error) <= 1.5)
            }
        }
    }

    @Test
    fun `first stable reading arrives within 150 ms of the pluck`() {
        val samples = stream(PitchTracker(sampleRate), pluck(82.41))
        val pluckMs = 500L
        val first = samples.first { it.frequencyHz != null }
        assertTrue("First reading at ${first.timestampMs - pluckMs} ms after pluck", first.timestampMs - pluckMs <= 150)
    }

    @Test
    fun `silence produces no readings`() {
        val samples = stream(PitchTracker(sampleRate), SyntheticSignals.noise(sampleRate * 2, amplitude = 0.0005))
        assertTrue(samples.all { it.frequencyHz == null })
    }

    @Test
    fun `muted audio is never analysed and the last pitch is held`() {
        val tracker = PitchTracker(sampleRate)
        val signal = pluck(110.0, seconds = 1.5)
        val muteStart = sampleRate // 1 s in: 0.5 s into the note
        // A loud foreign tone during the mute window must not leak into readings.
        val tone = SyntheticSignals.sine(659.26, sampleRate, sampleRate / 4, amplitude = 0.8)
        for (i in tone.indices) signal[muteStart + i] = tone[i]
        val samples = stream(tracker, signal) { position -> position in muteStart until muteStart + tone.size }

        samples.filter { it.frequencyHz != null }.forEach {
            val error = SyntheticSignals.cents(it.frequencyHz!!, 110.0)
            assertTrue("Leaked reading ${it.frequencyHz} Hz at ${it.timestampMs} ms", abs(error) <= 1.0)
        }
        val duringMute = samples.filter { it.timestampMs in 1050L..1250L }
        assertTrue(duringMute.isNotEmpty())
        assertTrue(duringMute.all { it.isHolding })
    }

    @Test
    fun `reset clears all state`() {
        val tracker = PitchTracker(sampleRate)
        stream(tracker, pluck(110.0, seconds = 1.0))
        tracker.reset()
        val chunk = FloatArray(tracker.hopSize)
        assertNull(tracker.process(chunk))
    }

    /**
     * Unplugged electric guitar through a raw phone mic: bright harmonics, weak fundamental,
     * long sustain, peaking around -60 dBFS over ~-70 dBFS microphone self-noise.
     */
    private fun quietElectricPluck(frequencyHz: Double, peakDbfs: Double, seed: Int = 5): FloatArray {
        val note = SyntheticSignals.string(frequencyHz, sampleRate, sampleRate * 3, fundamentalGainDb = -15.0, harmonics = 10, decayPerSecond = 0.4)
        val scale = Math.pow(10.0, peakDbfs / 20.0) / SyntheticSignals.rms(note.copyOfRange(0, sampleRate / 2))
        val signal = FloatArray(sampleRate / 2) + note
        val micNoise = SyntheticSignals.noise(signal.size, 0.0003 * Math.sqrt(3.0), seed)
        return FloatArray(signal.size) { (signal[it] * if (it >= sampleRate / 2) scale else 0.0).toFloat() + micNoise[it] }
    }

    @Test
    fun `quiet unplugged electric guitar is tracked accurately`() {
        for (f in listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)) {
            val samples = stream(PitchTracker(sampleRate), quietElectricPluck(f, peakDbfs = -60.0))
                .filter { it.timestampMs in 600L..2500L }
            val live = samples.filter { it.frequencyHz != null && !it.isHolding }
            assertTrue("Only ${live.size}/${samples.size} readings for quiet $f Hz", live.size >= samples.size * 0.8)
            live.forEach {
                val error = SyntheticSignals.cents(it.frequencyHz!!, f)
                assertTrue("quiet $f Hz off by $error cents", abs(error) <= 1.0)
            }
        }
    }

    @Test
    fun `microphone self-noise alone never produces a reading`() {
        for (amplitude in listOf(0.0003, 0.001, 0.003)) {
            val samples = stream(PitchTracker(sampleRate), SyntheticSignals.noise(sampleRate * 3, amplitude * Math.sqrt(3.0), seed = 9))
            assertTrue("noise at $amplitude produced a pitch", samples.all { it.frequencyHz == null })
        }
    }

    @Test
    fun `severely flat string in manual mode is tracked and reported accurately`() {
        val targets = listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)
        // Manual mode: locked to Low E (index 0). String is 150 cents flat (~75.5 Hz).
        val ctx = TunerAnalysisContext(targetsHz = targets, lockedIndex = 0)
        val tracker = PitchTracker(sampleRate, context = ctx)
        val flatLowE = SyntheticSignals.withCents(82.41, -150.0)
        val samples = stream(tracker, pluck(flatLowE, seconds = 1.5))
        val stable = samples.filter { it.frequencyHz != null && !it.isHolding }
        assertTrue("Severely flat string in manual mode must be tracked, got ${stable.size} samples", stable.size > 15)
        val error = SyntheticSignals.cents(stable.last().frequencyHz!!, flatLowE)
        assertTrue("Error should be <= 1.0 cent, was $error", abs(error) <= 1.0)
    }

    @Test
    fun `string 120 cents flat in auto mode is identified and tracked`() {
        val targets = listOf(82.41, 110.0, 146.83, 196.0, 246.94, 329.63)
        val ctx = TunerAnalysisContext(targetsHz = targets, lockedIndex = null)
        val tracker = PitchTracker(sampleRate, context = ctx)
        val flatA = SyntheticSignals.withCents(110.0, -120.0)
        val samples = stream(tracker, pluck(flatA, seconds = 1.5))
        val stable = samples.filter { it.frequencyHz != null && !it.isHolding }
        assertTrue("Flat string in auto mode must be tracked, got ${stable.size} samples", stable.size > 15)
        val error = SyntheticSignals.cents(stable.last().frequencyHz!!, flatA)
        assertTrue("Error should be <= 1.0 cent, was $error", abs(error) <= 1.0)
    }
}
