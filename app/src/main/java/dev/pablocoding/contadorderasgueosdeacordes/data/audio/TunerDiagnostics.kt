package dev.pablocoding.contadorderasgueosdeacordes.data.audio

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TunerDiagnostics"
private const val MAX_SECONDS = 120

/**
 * Developer-only capture of what the tuner hears, for analysing real-device behaviour offline.
 *
 * Active only in debuggable builds **and** when `files/tuner_debug/ENABLE` exists, which is created
 * over adb (`adb shell run-as <pkg> touch files/tuner_debug/ENABLE`). Writes the raw microphone
 * input as a 32-bit float WAV plus a CSV of the pipeline's per-hop output.
 */
@Singleton
class TunerDiagnostics @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val directory = File(context.filesDir, "tuner_debug")
    private val openSessions = AtomicInteger(0)
    private val _isRecording = MutableStateFlow(false)

    /** True while raw microphone audio is being written to disk (shown as a warning in the UI). */
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startSession(sampleRate: Int, description: String): Session? {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable || !File(directory, "ENABLE").exists()) return null
        return try {
            Session(directory, sampleRate, description) {
                _isRecording.value = openSessions.decrementAndGet() > 0
            }.also {
                openSessions.incrementAndGet()
                _isRecording.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start diagnostics capture", e)
            null
        }
    }

    class Session internal constructor(
        directory: File,
        private val sampleRate: Int,
        description: String,
        private val onClosed: () -> Unit
    ) : Closeable {
        private val stamp = System.currentTimeMillis()
        private val wav = RandomAccessFile(File(directory, "capture_$stamp.wav"), "rw")
        private val csv: BufferedWriter = File(directory, "capture_$stamp.csv").bufferedWriter()
        private val maxSamples = sampleRate.toLong() * MAX_SECONDS
        private var samplesWritten = 0L
        /** Set when a write fails (e.g. disk full): the capture stops, the tuner keeps running. */
        private var failed = false
        private var bytes = ByteBuffer.allocate(4096 * 4).order(ByteOrder.LITTLE_ENDIAN)

        init {
            wav.setLength(0)
            wav.write(ByteArray(44)) // header written on close
            csv.write("# $description\n")
            csv.write("t_ms,level,clarity,freq_hz,holding,signal,settled,string_hint,note\n")
            Log.i(TAG, "Capturing to capture_$stamp.wav ($description)")
        }

        fun writeAudio(buffer: FloatArray, count: Int) {
            if (failed || samplesWritten >= maxSamples) return
            val n = minOf(count.toLong(), maxSamples - samplesWritten).toInt()
            if (bytes.capacity() < n * 4) bytes = ByteBuffer.allocate(n * 4).order(ByteOrder.LITTLE_ENDIAN)
            bytes.clear()
            for (i in 0 until n) bytes.putFloat(buffer[i])
            guarded { wav.write(bytes.array(), 0, n * 4) }
            if (!failed) samplesWritten += n
        }

        fun writeSample(sample: PitchSample) {
            if (failed || samplesWritten >= maxSamples) return
            guarded {
                csv.write("${sample.timestampMs},${sample.level},${sample.clarity},${sample.frequencyHz ?: ""}," +
                    "${sample.isHolding},${sample.isSignalPresent},${sample.isSettled},${sample.stringHint ?: ""},${sample.isNoteActive}\n")
            }
        }

        private inline fun guarded(write: () -> Unit) {
            try {
                write()
            } catch (e: IOException) {
                Log.e(TAG, "Diagnostics capture stopped", e)
                failed = true
            }
        }

        override fun close() {
            try {
                val dataBytes = samplesWritten * 4
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                    put("RIFF".toByteArray()); putInt((36 + dataBytes).toInt()); put("WAVE".toByteArray())
                    put("fmt ".toByteArray()); putInt(16); putShort(3); putShort(1) // IEEE float, mono
                    putInt(sampleRate); putInt(sampleRate * 4); putShort(4); putShort(32)
                    put("data".toByteArray()); putInt(dataBytes.toInt())
                }
                wav.seek(0)
                wav.write(header.array())
            } catch (e: IOException) {
                Log.e(TAG, "Could not finalise diagnostics capture", e)
            } finally {
                runCatching { wav.close() }
                runCatching { csv.close() }
                onClosed()
                Log.i(TAG, "Capture finished: ${samplesWritten / sampleRate} s")
            }
        }
    }
}
