package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.StrumDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioDetector"
private const val SAMPLE_RATE = 44100

@Singleton
class AudioDetector @Inject constructor(
    private val timeSource: TimeSource
) {
    /**
     * Secondary constructor for direct instantiation without dependency injection.
     */
    constructor() : this(TimeSource { SystemClock.elapsedRealtime() })

    /**
     * Emits [Unit] each time a guitar strum is detected.
     *
     * @param sensitivity 0.0 = only loud strums count, 1.0 = even quiet strums count.
     * @param debounceMs  Minimum milliseconds between two counted strums. Default 350ms.
     * @throws AudioDetectionException when microphone access is denied or hardware fails.
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
        if (minBuffer <= 0) {
            throw AudioDetectionException.MicrophoneUnavailable(
                "AudioRecord minBufferSize query returned invalid buffer size: $minBuffer"
            )
        }
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
            Log.w(TAG, "Permission denied when creating AudioRecord", e)
            throw AudioDetectionException.PermissionDenied(
                "Microphone permission is required to detect guitar strums",
                e
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid AudioRecord parameters", e)
            throw AudioDetectionException.MicrophoneUnavailable(
                "Audio hardware does not support requested configuration",
                e
            )
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            Log.e(TAG, "AudioRecord state not initialized (hardware unavailable or busy)")
            throw AudioDetectionException.MicrophoneUnavailable(
                "Microphone hardware is unavailable or in use by another application"
            )
        }

        val strumDetector = StrumDetector(sensitivity, debounceMs)
        val buffer = ShortArray(bufferSize)

        try {
            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw AudioDetectionException.RecordingFailed(
                    "AudioRecord could not start recording (state: ${audioRecord.recordingState})"
                )
            }
            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val nowMs = timeSource.elapsedRealtime()
                    if (strumDetector.process(buffer, read, nowMs)) {
                        emit(Unit)
                    }
                } else if (read < 0) {
                    // AudioRecord error code returned (e.g., ERROR_INVALID_OPERATION, ERROR_DEAD_OBJECT)
                    Log.e(TAG, "AudioRecord read returned error code: $read")
                    throw AudioDetectionException.RecordingFailed("AudioRecord read error: $read")
                }
            }
        } finally {
            try {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while stopping AudioRecord", e)
            }
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)
}
