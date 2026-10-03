package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.AudioDetectionException
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TunerMicrophone"
private const val FALLBACK_SAMPLE_RATE = 48_000
private const val MIN_BUFFER_BYTES = 16_384 * Float.SIZE_BYTES

/** A started microphone capture delivering mono float PCM in [-1, 1]. */
interface PcmInputStream : Closeable {
    val sampleRate: Int

    /** Human-readable capture configuration (source, device, rate) for diagnostics. */
    val description: String get() = "rate=$sampleRate"

    /** Blocking read; returns the number of samples read, or a negative AudioRecord error code. */
    fun read(buffer: FloatArray, count: Int = buffer.size): Int

    /**
     * True when the OS is feeding us silence because another app took the microphone
     * (concurrent capture, Android 10+). Some devices also output exact digital zeros in a
     * quiet room, so this asks the OS instead of guessing from the samples.
     */
    fun isSilenced(): Boolean
}

/**
 * Opens the microphone configured for pitch accuracy rather than voice:
 * - `UNPROCESSED` source (tried even when not advertised), else `VOICE_RECOGNITION` (both avoid
 *   the AGC and noise suppression that `MIC` applies on many devices),
 * - the device's native sample rate (no resampler in the path),
 * - 32-bit float samples,
 * - the built-in microphone even when a Bluetooth headset is connected (SCO mics are 8–16 kHz).
 */
@Singleton
class TunerMicrophone @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** @throws AudioDetectionException when permission is missing or no configuration works. */
    fun open(): PcmInputStream {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw AudioDetectionException.PermissionDenied("Microphone permission is required to use the tuner")
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nativeRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()
            ?.takeIf { it in 8_000..192_000 }
            ?: FALLBACK_SAMPLE_RATE
        val supportsUnprocessed = audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"

        // UNPROCESSED first even when not advertised: the Pixel 9 Pro XL reports no support yet
        // opens it fine. Creation simply fails on devices that really lack it, and we fall back.
        val sources = listOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        Log.i(TAG, "UNPROCESSED advertised=$supportsUnprocessed, trying sources=${sources.map(::sourceName)}")
        val rates = listOf(nativeRate, 48_000, 44_100).distinct()
        val builtInMic = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

        // A configuration that is created but won't start may be a device quirk (try the next one)
        // or another app holding the microphone (then every configuration fails to start).
        var startFailed = false
        for (source in sources) {
            for (rate in rates) {
                val record = tryCreate(source, rate) ?: continue
                builtInMic?.let { record.setPreferredDevice(it) }
                try {
                    record.startRecording()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "startRecording failed for source=$source rate=$rate", e)
                }
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    record.release()
                    startFailed = true
                    continue
                }
                val routed = record.routedDevice
                // AudioDeviceInfo.address needs API 28.
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "/${routed?.address}" else ""
                val description = "source=${sourceName(source)} rate=${record.sampleRate} " +
                    "device=${routed?.productName}$address type=${routed?.type}"
                Log.i(TAG, "Tuner capture: $description")
                return AudioRecordStream(record, description)
            }
        }
        throw AudioDetectionException.MicrophoneUnavailable(
            if (startFailed) "Microphone is in use by another application"
            else "No supported microphone configuration for the tuner"
        )
    }

    @SuppressLint("MissingPermission") // Checked in open()
    private fun tryCreate(source: Int, sampleRate: Int): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        if (minBuffer <= 0) return null
        val record = try {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, MIN_BUFFER_BYTES))
                .build()
        } catch (e: SecurityException) {
            throw AudioDetectionException.PermissionDenied("Microphone permission is required to use the tuner", e)
        } catch (e: UnsupportedOperationException) {
            Log.d(TAG, "Unsupported config source=$source rate=$sampleRate", e)
            return null
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Invalid config source=$source rate=$sampleRate", e)
            return null
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        return record
    }

    private fun sourceName(source: Int) = when (source) {
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        else -> source.toString()
    }

    private class AudioRecordStream(
        private val record: AudioRecord,
        override val description: String
    ) : PcmInputStream {
        override val sampleRate: Int = record.sampleRate

        override fun read(buffer: FloatArray, count: Int): Int =
            record.read(buffer, 0, count, AudioRecord.READ_BLOCKING)

        override fun isSilenced(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                record.activeRecordingConfiguration?.isClientSilenced == true

        override fun close() {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Exception while stopping tuner AudioRecord", e)
            }
            record.release()
        }
    }
}
