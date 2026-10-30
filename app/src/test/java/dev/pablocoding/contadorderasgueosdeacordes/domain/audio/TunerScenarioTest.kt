package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.RealisticGuitarSignals.SAMPLE_RATE
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.RealisticGuitarSignals.STANDARD
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * End-to-end scenarios on realistic simulated guitar audio (see [RealisticGuitarSignals]).
 * The non-negotiable rule throughout: never show a wrong note.
 *
 * The tuner reports each note's early sustain (median 0.25–0.9 s after the pluck). The simulated
 * pluck starts 8¢ sharp and glides down (τ = 150 ms), so that window still carries ~1¢ of glide:
 * median limits are 1.5¢ (2¢ with a sympathetic string), not the detector's sub-cent precision,
 * which the calibration-tone replay and the unit tests cover.
 */
class TunerScenarioTest {

    private val tuning = TuningLibrary.standard

    data class Outcome(
        val coverage: Double,          // share of hops in the note window with a live reading
        val wrongString: Int,          // live readings attributed to another string
        val falseBeforePluck: Int,     // readings before anything was plucked
        val medianCents: Double,
        val worstSettledCents: Double  // worst settled-reading error after the first second
    )

    /**
     * One pluck of [stringIndex] at [pluckS] (default 1 s; 2.5 s with hum, like opening the tuner
     * and plucking a moment later), at [stringDbfs] scaled by the string's loudness.
     */
    private fun single(stringIndex: Int, stringDbfs: Double, noiseDbfs: Double, humDbfs: Double? = null): Outcome {
        val s = STANDARD[stringIndex]
        val pluckS = if (humDbfs != null) 2.5 else 1.0
        val note = RealisticGuitarSignals.pluck(s.frequencyHz, s.inharmonicity, 3.0, seed = stringIndex + 1)
        val gain = RealisticGuitarSignals.gainFor(note, stringDbfs) * s.loudness
        val audio = RealisticGuitarSignals.scene(pluckS + 3.0, listOf(Triple((pluckS * SAMPLE_RATE).toInt(), note, gain)), noiseDbfs, humDbfs)
        return evaluate(audio, stringIndex, pluckMs = (pluckS * 1000).toLong())
    }

    private fun evaluate(audio: FloatArray, stringIndex: Int, pluckMs: Long, windowMs: Long = 2500): Outcome {
        val frames = TunerReplay.run(audio, SAMPLE_RATE, tuning)
        val before = frames.count { it.timeMs < pluckMs && it.reading.hasPitch }
        val window = frames.filter { it.timeMs in pluckMs + 100..pluckMs + windowMs }
        val live = window.filter { it.reading.hasPitch && !it.reading.isHolding }
        val wrong = live.count { it.reading.targetStringIndex != stringIndex }
        val cents = live.filter { it.reading.targetStringIndex == stringIndex }.map { it.reading.cents!! }.sorted()
        val late = live.filter { it.sample.isSettled && it.timeMs >= pluckMs + 1000 && it.reading.targetStringIndex == stringIndex }
        return Outcome(
            coverage = live.size.toDouble() / window.size.coerceAtLeast(1),
            wrongString = wrong,
            falseBeforePluck = before,
            medianCents = if (cents.isEmpty()) Double.NaN else cents[cents.size / 2],
            worstSettledCents = late.maxOfOrNull { abs(it.reading.cents!!) } ?: 0.0
        )
    }

    private fun report(name: String, outcomes: List<Outcome>) {
        println("$name: " + outcomes.mapIndexed { i, o ->
            "${6 - i}: ${"%3.0f".format(o.coverage * 100)}% med ${"%+.1f".format(o.medianCents)} worst ${"%.1f".format(o.worstSettledCents)}" +
                (if (o.wrongString > 0) " WRONG${o.wrongString}" else "") + (if (o.falseBeforePluck > 0) " FALSE${o.falseBeforePluck}" else "")
        }.joinToString(" | "))
    }

    @Test
    fun `acoustic-level guitar in a quiet room is read on every string`() {
        val outcomes = STANDARD.indices.map { single(it, stringDbfs = -35.0, noiseDbfs = -62.0) }
        report("acoustic", outcomes)
        outcomes.forEach { o ->
            assertEquals(0, o.wrongString + o.falseBeforePluck)
            assertTrue("coverage ${o.coverage}", o.coverage >= 0.85)
            assertTrue("median ${o.medianCents}", abs(o.medianCents) <= 1.5)
            // The plain G string is the stiffest: its late readings stay within ~2¢.
            assertTrue("worst ${o.worstSettledCents}", o.worstSettledCents <= 2.5)
        }
    }

    @Test
    fun `unplugged electric in a quiet room - thin strings included`() {
        val outcomes = STANDARD.indices.map { single(it, stringDbfs = -60.0, noiseDbfs = -72.0) }
        report("unplugged quiet", outcomes)
        outcomes.forEach { o ->
            assertEquals(0, o.wrongString + o.falseBeforePluck)
            assertTrue("coverage ${o.coverage}", o.coverage >= 0.35)
            assertTrue("median ${o.medianCents}", abs(o.medianCents) <= 1.5)
        }
    }

    @Test
    fun `mains hum is never shown as a note and does not hide the strings`() {
        val outcomes = STANDARD.indices.map { single(it, stringDbfs = -60.0, noiseDbfs = -72.0, humDbfs = -68.0) }
        report("hum", outcomes)
        outcomes.forEach { o ->
            assertEquals("hum displayed before any pluck", 0, o.falseBeforePluck)
            assertEquals(0, o.wrongString)
            assertTrue("coverage ${o.coverage}", o.coverage >= 0.3)
            assertTrue("median ${o.medianCents}", abs(o.medianCents) <= 1.5)
        }
    }

    @Test
    fun `loud strings with an amp hum are read accurately`() {
        val outcomes = STANDARD.indices.map { single(it, stringDbfs = -40.0, noiseDbfs = -64.0, humDbfs = -58.0) }
        report("loud + hum", outcomes)
        outcomes.forEach { o ->
            assertEquals(0, o.wrongString + o.falseBeforePluck)
            assertTrue("coverage ${o.coverage}", o.coverage >= 0.6)
            assertTrue("median ${o.medianCents}", abs(o.medianCents) <= 1.5)
        }
    }

    @Test
    fun `high E is identified even though the open low E rings in sympathy`() {
        // Real capture: plucking the high E makes the open low E (its 4th sub-harmonic) ring
        // louder than the high E within ~400 ms.
        val e4 = STANDARD[5]
        val e2 = STANDARD[0]
        val pluck = RealisticGuitarSignals.pluck(e4.frequencyHz, e4.inharmonicity, 3.0, seed = 6)
        val sympathetic = RealisticGuitarSignals.pluck(e2.frequencyHz, e2.inharmonicity, 2.7, glideCents = 0.0, seed = 7)
            .also { s -> for (i in s.indices) s[i] *= minOf(1.0, i / (0.4 * SAMPLE_RATE)) } // builds up over 400 ms
        val g = RealisticGuitarSignals.gainFor(pluck, -45.0)
        val audio = RealisticGuitarSignals.scene(
            4.0,
            listOf(Triple(SAMPLE_RATE, pluck, g), Triple(SAMPLE_RATE + SAMPLE_RATE / 10, sympathetic, g * 1.5)),
            noiseDbfs = -68.0
        )
        val o = evaluate(audio, stringIndex = 5, pluckMs = 1000)
        report("sympathetic", listOf(o))
        assertEquals("readings attributed to the sympathetic low E", 0, o.wrongString)
        assertTrue(o.coverage >= 0.5)
        assertTrue(abs(o.medianCents) <= 2.0)
    }

    @Test
    fun `live reading follows each pluck on the right string and reaches in-tune`() {
        val outcomes = STANDARD.indices.map { i ->
            val s = STANDARD[i]
            val note = RealisticGuitarSignals.pluck(s.frequencyHz, s.inharmonicity, 3.0, seed = i + 1)
            val audio = RealisticGuitarSignals.scene(4.0,
                listOf(Triple(SAMPLE_RATE, note, RealisticGuitarSignals.gainFor(note, -48.0) * s.loudness)), noiseDbfs = -70.0)
            val frames = TunerReplay.run(audio, SAMPLE_RATE, tuning, readingMode = TunerReadingMode.LIVE)
            val live = frames.filter { it.timeMs in 1100L..3500L && it.reading.hasPitch && !it.reading.isHolding }
            assertTrue("string ${6 - i}: only ${live.size} readings", live.size >= 40)
            assertEquals("string ${6 - i}: readings on the wrong string", 0, live.count { it.reading.targetStringIndex != i })
            assertTrue("string ${6 - i}: first reading within 250 ms of the pluck", live.first().timeMs <= 1250)
            assertTrue("string ${6 - i}: never in tune", live.any { it.reading.isInTune })
            // The simulated pluck starts 8 cents sharp: live mode shows that glide, steady mode doesn't.
            assertTrue("string ${6 - i}: attack glide not shown", live.first().reading.cents!! > 1.5)
            live.last().reading.cents!!
        }
        println("live final cents: " + outcomes.joinToString { "%+.1f".format(it) })
        outcomes.forEach { assertTrue(abs(it) <= 1.5) }
    }

    @Test
    fun `tuning string by string while previous strings keep ringing`() {
        // 6 → 1, one pluck every 2.5 s, each string still ringing when the next is plucked.
        val events = STANDARD.mapIndexed { i, s ->
            val note = RealisticGuitarSignals.pluck(s.frequencyHz, s.inharmonicity, 6.0, decayPerSecond = 0.3, seed = 20 + i)
            Triple((1.0 + 2.5 * i).let { (it * SAMPLE_RATE).toInt() }, note, RealisticGuitarSignals.gainFor(note, -48.0) * s.loudness)
        }
        val audio = RealisticGuitarSignals.scene(17.0, events, noiseDbfs = -70.0)
        val frames = TunerReplay.run(audio, SAMPLE_RATE, tuning)
        STANDARD.indices.forEach { i ->
            val from = (1000 + 2500 * i).toLong()
            val live = frames.filter { it.timeMs in from + 300..from + 2400 && it.reading.hasPitch && !it.reading.isHolding }
            val wrong = live.count { it.reading.targetStringIndex != i }
            val cents = live.filter { it.reading.targetStringIndex == i }.map { it.reading.cents!! }.sorted()
            println("sequence string ${6 - i}: ${live.size} readings, wrong=$wrong, median ${if (cents.isEmpty()) "n/a" else "%+.1f".format(cents[cents.size / 2])}")
            assertTrue("string ${6 - i}: only ${live.size} readings", live.size >= 30)
            assertTrue("string ${6 - i}: $wrong of ${live.size} readings on the wrong string", wrong <= live.size / 20)
            assertTrue(abs(cents[cents.size / 2]) <= 1.5)
        }
    }
}
