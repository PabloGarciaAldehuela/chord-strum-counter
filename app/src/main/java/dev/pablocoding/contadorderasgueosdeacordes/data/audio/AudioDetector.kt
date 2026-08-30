package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

private const val SAMPLE_RATE = 44100

// RMS amplitude thresholds on a 16-bit PCM scale (max = 32767)
// sensitivity 1.0f = very sensitive (triggers on quiet strums)
// sensitivity 0.0f = least sensitive (only loud strums trigger)
private const val THRESHOLD_QUIET = 400.0   // high sensitivity end
private const val THRESHOLD_LOUD  = 6000.0  // low sensitivity end

@Singleton
class AudioDetector @Inject constructor() {

    /**
     * Emits [Unit] each time a guitar strum is detected.
     *
     * @param sensitivity 0.0 = only loud strums count, 1.0 = even quiet strums count.
     * @param debounceMs  Minimum milliseconds between two counted strums. Default 350ms.
     */
    fun detectStrums(sensitivity: Float = 0.6f, debounceMs: Long = 350L): Flow<Unit> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, 2048)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return@flow
        }

        // Map sensitivity [0,1] → threshold [LOUD, QUIET] (inverse relationship)
        val threshold = THRESHOLD_LOUD - (sensitivity.coerceIn(0f, 1f) * (THRESHOLD_LOUD - THRESHOLD_QUIET))

        var lastDetectionTime = 0L
        val buffer = ShortArray(bufferSize)

        try {
            audioRecord.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val rms = rms(buffer, read)
                    val now = System.currentTimeMillis()
                    if (rms > threshold && now - lastDetectionTime > debounceMs) {
                        lastDetectionTime = now
                        emit(Unit)
                    }
                } else if (read < 0) {
                    // AudioRecord error code returned (e.g., ERROR_INVALID_OPERATION)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun rms(buffer: ShortArray, read: Int): Double {
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        return sqrt(sum / read)
    }
}
