package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.StrumDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

private const val SAMPLE_RATE = 44100

@Singleton
class AudioDetector @Inject constructor(
    private val timeSource: TimeSource
) {
    /**
     * Emits [Unit] each time a guitar strum is detected.
     *
     * @param sensitivity 0.0 = only loud strums count, 1.0 = even quiet strums count.
     * @param debounceMs  Minimum milliseconds between two counted strums. Default 350ms.
     */
    fun detectStrums(
        sensitivity: Float = StrumDetector.DEFAULT_SENSITIVITY,
        debounceMs: Long = StrumDetector.DEFAULT_DEBOUNCE_MS
    ): Flow<Unit> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, 2048)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            return@flow
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return@flow
        }

        val strumDetector = StrumDetector(sensitivity, debounceMs)
        val buffer = ShortArray(bufferSize)

        try {
            audioRecord.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val nowMs = timeSource.elapsedRealtime()
                    if (strumDetector.process(buffer, read, nowMs)) {
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
}
