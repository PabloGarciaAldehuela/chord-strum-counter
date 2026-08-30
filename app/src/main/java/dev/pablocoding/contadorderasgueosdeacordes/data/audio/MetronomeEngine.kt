package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dev.pablocoding.contadorderasgueosdeacordes.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val SAMPLE_RATE = 44100

@Singleton
class MetronomeEngine @Inject constructor(
    @param:ApplicationScope private val appScope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(1)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private var metronomeJob: Job? = null
    private var currentBpm: Int = 80
    private var beatsPerMeasure: Int = 4

    // Pre-synthesized audio tracks
    private var accentTrack: AudioTrack? = null
    private var regularTrack: AudioTrack? = null

    init {
        initAudioTracks()
    }

    private fun initAudioTracks() {
        try {
            val accentSamples = generateClickPcm(frequencyHz = 1600.0, durationMs = 25, decay = 80.0)
            val regularSamples = generateClickPcm(frequencyHz = 1050.0, durationMs = 18, decay = 110.0)

            accentTrack = createStaticTrack(accentSamples)
            regularTrack = createStaticTrack(regularSamples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createStaticTrack(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

        track.write(samples, 0, samples.size)
        return track
    }

    /**
     * Procedurally synthesizes a pleasant woodblock-style click with an exponential decay envelope.
     */
    private fun generateClickPcm(frequencyHz: Double, durationMs: Int, decay: Double): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val maxAmp = 28000.0 // Near max 16-bit without clipping

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-decay * t)
            val sample = sin(2.0 * PI * frequencyHz * t) * envelope * maxAmp
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    fun start(bpm: Int = currentBpm, beats: Int = beatsPerMeasure) {
        currentBpm = bpm.coerceIn(40, 240)
        beatsPerMeasure = beats
        _isPlaying.value = true

        metronomeJob?.cancel()
        metronomeJob = appScope.launch(Dispatchers.Default) {
            var beat = 1
            val intervalNanos = (60_000_000_000L / currentBpm)
            var nextTickTime = System.nanoTime()

            while (isActive && _isPlaying.value) {
                // Play click sound
                playClick(beat == 1)
                _currentBeat.value = beat

                // Increment beat
                beat = if (beat >= beatsPerMeasure) 1 else beat + 1

                // Precision delta sleep
                nextTickTime += (60_000_000_000L / currentBpm)
                val now = System.nanoTime()
                val sleepNanos = nextTickTime - now

                if (sleepNanos > 0) {
                    val sleepMs = sleepNanos / 1_000_000L
                    val sleepRemNanos = (sleepNanos % 1_000_000L).toInt()
                    delay(sleepMs)
                    if (sleepRemNanos > 0) {
                        val targetNano = System.nanoTime() + sleepRemNanos
                        while (System.nanoTime() < targetNano) {
                            // High precision spin for sub-millisecond accuracy
                        }
                    }
                } else {
                    // System fell behind, catch up to current time
                    nextTickTime = System.nanoTime()
                }
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        metronomeJob?.cancel()
        metronomeJob = null
        _currentBeat.value = 1
    }

    fun setBpm(bpm: Int) {
        currentBpm = bpm.coerceIn(40, 240)
        if (_isPlaying.value) {
            // Restart loop with new tempo immediately
            start(currentBpm, beatsPerMeasure)
        }
    }

    private fun playClick(isAccent: Boolean) {
        val track = if (isAccent) accentTrack else regularTrack
        track?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.reloadStaticData()
                it.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
