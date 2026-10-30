package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

enum class TuneDirection(val label: String) {
    IN_TUNE("In tune"),
    TUNE_UP("Tune up"),
    TUNE_DOWN("Tune down"),
    TUNE_UP_A_LOT("Tune up a lot"),
    TUNE_DOWN_A_LOT("Tune down a lot")
}

/** Colour band of the pitch trace and bubble. */
enum class DeviationLevel { IN_TUNE, CLOSE, FAR }

data class NoteLabel(val name: String, val octave: Int)

/** Pure display helpers for the tuner screen (kept out of composables so they can be unit tested). */
object TunerFormatting {

    const val FAR_OFF_CENTS = 50.0
    const val CLOSE_CENTS = 15.0
    const val DISPLAY_RANGE_CENTS = 50.0
    private const val MINUS = "−"

    fun direction(cents: Double, isInTune: Boolean): TuneDirection = when {
        isInTune -> TuneDirection.IN_TUNE
        cents <= -FAR_OFF_CENTS -> TuneDirection.TUNE_UP_A_LOT
        cents >= FAR_OFF_CENTS -> TuneDirection.TUNE_DOWN_A_LOT
        cents < 0 -> TuneDirection.TUNE_UP
        else -> TuneDirection.TUNE_DOWN
    }

    fun deviationLevel(cents: Double, toleranceCents: Double): DeviationLevel = when {
        abs(cents) <= toleranceCents -> DeviationLevel.IN_TUNE
        abs(cents) <= CLOSE_CENTS -> DeviationLevel.CLOSE
        else -> DeviationLevel.FAR
    }

    /** "+7", "−12", "0"; beyond ±99 cents shows "≪" (very flat) or "≫" (very sharp). */
    fun centsLabel(cents: Double): String {
        val rounded = cents.roundToInt()
        return when {
            rounded <= -100 -> "≪"
            rounded >= 100 -> "≫"
            rounded < 0 -> "$MINUS${-rounded}"
            rounded > 0 -> "+$rounded"
            else -> "0"
        }
    }

    /** Input level mapped from −80…−30 dBFS onto 0…1 for the listening meter. */
    fun levelFraction(rms: Double): Float {
        if (rms <= 0.0) return 0f
        val dbfs = 20.0 * kotlin.math.log10(rms)
        return ((dbfs + 80.0) / 50.0).coerceIn(0.0, 1.0).toFloat()
    }

    fun frequencyLabel(frequencyHz: Double): String = String.format(Locale.US, "%.1f Hz", frequencyHz)

    /**
     * Horizontal position in [-1, 1] for a deviation. The curve magnifies the region near zero
     * so a few cents are clearly visible while ±50¢ still fits on screen.
     */
    fun displayPosition(cents: Double): Float {
        val normalized = min(abs(cents), DISPLAY_RANGE_CENTS) / DISPLAY_RANGE_CENTS
        return (sign(cents) * normalized.pow(0.7)).toFloat()
    }

    fun noteLabel(midi: Int, preferFlats: Boolean, naming: NoteNaming): NoteLabel =
        NoteLabel(PitchMath.noteName(midi, preferFlats, naming), PitchMath.octave(midi))

    /** TalkBack phrase, e.g. "E 2, 5 cents flat, tune up" or "A 2, in tune". */
    fun accessibilityDescription(reading: TunerReading, tuning: Tuning, naming: NoteNaming): String {
        val target = reading.targetMidi
        val cents = reading.cents
        if (!reading.hasPitch || target == null || cents == null) return "Listening. Pluck a string."
        val note = noteLabel(target, tuning.preferFlats, naming)
        val name = "${note.name} ${note.octave}"
        if (reading.isInTune) return "$name, in tune"
        val amount = abs(cents).roundToInt()
        val side = if (cents < 0) "flat" else "sharp"
        val direction = direction(cents, isInTune = false).label.lowercase()
        return "$name, $amount cents $side, $direction"
    }
}
