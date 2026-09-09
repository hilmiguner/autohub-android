package com.autohub.android.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Generates a deterministic 10-second PCM WAV tone for manual playback tests. */
object TestToneFile {
    private const val FILE_NAME = "autohub_test_tone_10s.wav"
    private const val SAMPLE_RATE_HZ = 44_100
    private const val DURATION_SECONDS = 10
    private const val FREQUENCY_HZ = 440.0
    private const val CHANNEL_COUNT = 1
    private const val BITS_PER_SAMPLE = 16
    private const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
    private const val AMPLITUDE = 0.20
    private const val WAV_HEADER_BYTES = 44

    fun getOrCreate(context: Context): Uri {
        val file = File(context.cacheDir, FILE_NAME)
        val expectedLength = WAV_HEADER_BYTES.toLong() + dataSizeBytes()

        if (!file.exists() || file.length() != expectedLength) {
            writeTone(file)
        }

        return Uri.fromFile(file)
    }

    private fun writeTone(file: File) {
        val sampleCount = SAMPLE_RATE_HZ * DURATION_SECONDS
        val dataSize = dataSizeBytes().toInt()
        val byteRate = SAMPLE_RATE_HZ * CHANNEL_COUNT * BYTES_PER_SAMPLE
        val blockAlign = CHANNEL_COUNT * BYTES_PER_SAMPLE

        val buffer = ByteBuffer
            .allocate(WAV_HEADER_BYTES + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(CHANNEL_COUNT.toShort())
        buffer.putInt(SAMPLE_RATE_HZ)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)

        repeat(sampleCount) { sampleIndex ->
            val phase = 2.0 * PI * FREQUENCY_HZ * sampleIndex / SAMPLE_RATE_HZ
            val sample = (sin(phase) * Short.MAX_VALUE * AMPLITUDE)
                .toInt()
                .toShort()
            buffer.putShort(sample)
        }

        file.outputStream().buffered().use { output ->
            output.write(buffer.array())
        }
    }

    private fun dataSizeBytes(): Long =
        SAMPLE_RATE_HZ.toLong() * DURATION_SECONDS * CHANNEL_COUNT * BYTES_PER_SAMPLE
}
