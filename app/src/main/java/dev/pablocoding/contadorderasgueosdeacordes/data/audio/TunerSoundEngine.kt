package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TunerSoundEngine"
private const val SAMPLE_RATE = ToneSynthesisMath.DEFAULT_SAMPLE_RATE
private const val TONE_DURATION_MS = 1800
private const val CHIME_DURATION_MS = 380

/** Extra time after a sound ends before the tuner listens again (speaker → room → mic echo). */
private const val ECHO_TAIL_MS = 250

/**
 * Plays the tuner's reference tones and in-tune chime, and tells the pitch pipeline when to stop
 * listening so the tuner never tunes to its own speaker.
 */
@Singleton
class TunerSoundEngine @Inject constructor(
    private val timeSource: TimeSource
) {
    private val lock = Any()
    private var track: AudioTrack? = null
    private val chimeSamples: ShortArray by lazy { ToneSynthesisMath.generateChime(SAMPLE_RATE, CHIME_DURATION_MS) }

    @Volatile
    private var mutedUntilMs = 0L

    /** True while one of our sounds (plus its echo tail) may reach the microphone. */
    fun isDetectionMuted(): Boolean = timeSource.elapsedRealtime() < mutedUntilMs

    suspend fun playReferenceTone(frequencyHz: Double) {
        val samples = withContext(Dispatchers.Default) {
            ToneSynthesisMath.generatePluckedTone(frequencyHz, SAMPLE_RATE, TONE_DURATION_MS)
        }
        play(samples, TONE_DURATION_MS, AudioAttributes.CONTENT_TYPE_MUSIC)
    }

    fun playInTuneChime() {
        play(chimeSamples, CHIME_DURATION_MS, AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    fun stop() {
        synchronized(lock) {
            releaseTrack()
            mutedUntilMs = 0L
        }
    }

    private fun play(samples: ShortArray, durationMs: Int, contentType: Int) {
        synchronized(lock) {
            releaseTrack()
            mutedUntilMs = timeSource.elapsedRealtime() + durationMs + ECHO_TAIL_MS
            try {
                val newTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(contentType)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                newTrack.write(samples, 0, samples.size)
                newTrack.play()
                track = newTrack
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play tuner sound", e)
                mutedUntilMs = 0L
            }
        }
    }

    private fun releaseTrack() {
        track?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Error stopping tuner sound", e)
            }
            it.release()
        }
        track = null
    }
}
