package com.example.sleepandwhitenoise.tracking

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.sleepandwhitenoise.data.EventType
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

object Mic {
    const val RATE = 16_000
    const val CHUNK = RATE / 10 // 100 ms

    @SuppressLint("MissingPermission") // callers check RECORD_AUDIO first
    fun open(): AudioRecord? = runCatching {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, max(min, CHUNK * 4),
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); null } else rec
    }.getOrNull()

    /** Approximate loudness on a 0..100 scale (dBFS shifted by +90, clamped). */
    fun level(buf: ShortArray, n: Int): Float {
        if (n <= 0) return 0f
        var sum = 0.0
        for (i in 0 until n) { val v = buf[i].toDouble(); sum += v * v }
        val rms = sqrt(sum / n)
        val dbfs = if (rms < 1) -90.0 else 20 * log10(rms / 32768.0)
        return (dbfs + 90).toFloat().coerceIn(0f, 100f)
    }

    /** Zero-crossing rate: low values mean energy is concentrated in low frequencies. */
    fun zcr(buf: ShortArray, n: Int): Float {
        if (n < 2) return 0f
        var c = 0
        for (i in 1 until n) if ((buf[i] >= 0) != (buf[i - 1] >= 0)) c++
        return c / n.toFloat()
    }

    fun writeWav(file: File, pcm: ShortArray, n: Int) {
        file.parentFile?.mkdirs()
        val dataBytes = n * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(RATE); putInt(RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes)
        }
        val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) body.putShort(pcm[i])
        FileOutputStream(file).use { it.write(header.array()); it.write(body.array()) }
    }
}

/**
 * Lightweight on-device event detector. It tracks an adaptive noise floor and reports bursts that
 * rise clearly above it. Classification is a heuristic estimate, never a diagnosis.
 */
class EventDetector(private val onEvent: (Detected) -> Unit) {
    data class Detected(
        val startOffsetMs: Long, val durationMs: Long, val type: EventType,
        val peak: Float, val confidence: Float, val startChunk: Long, val endChunk: Long,
    )

    private var baseline = -1f
    private var inEvent = false
    private var aboveCount = 0
    private var belowCount = 0
    private var eventStartChunk = 0L
    private var peak = 0f
    private var zcrSum = 0f
    private var zcrN = 0
    private val recentStarts = ArrayDeque<Long>()

    val noiseFloor: Float get() = baseline.coerceAtLeast(0f)

    /** Feed one 100 ms chunk. [chunkIndex] counts listening chunks since the session started. */
    fun feed(level: Float, zcr: Float, chunkIndex: Long) {
        if (baseline < 0) baseline = level
        val threshold = max(baseline + 12f, 32f)
        if (!inEvent) {
            baseline += (level - baseline) * if (level < baseline) 0.05f else 0.005f
            if (level > threshold) {
                aboveCount++
                if (aboveCount >= 3) {
                    inEvent = true; belowCount = 0
                    eventStartChunk = chunkIndex - 2; peak = level; zcrSum = zcr; zcrN = 1
                }
            } else aboveCount = 0
        } else {
            peak = max(peak, level); zcrSum += zcr; zcrN++
            if (level < baseline + 6f) belowCount++ else belowCount = 0
            val tooLong = chunkIndex - eventStartChunk > 300 // cap events at 30 s
            if (belowCount >= 8 || tooLong) finish(chunkIndex)
        }
    }

    fun flush(chunkIndex: Long) { if (inEvent) finish(chunkIndex) }

    private fun finish(endChunk: Long) {
        inEvent = false; aboveCount = 0
        val durChunks = (endChunk - eventStartChunk - belowCount).coerceAtLeast(3)
        val durationMs = durChunks * 100
        val avgZcr = zcrSum / zcrN.coerceAtLeast(1)
        recentStarts.addLast(eventStartChunk)
        while (recentStarts.isNotEmpty() && eventStartChunk - recentStarts.first() > 600) recentStarts.removeFirst()
        // Rhythmic, low-frequency bursts repeating every 2–8 s look like snoring.
        val rhythmic = recentStarts.zipWithNext { a, b -> b - a }.count { it in 20..80 } >= 2
        val (type, conf) = when {
            avgZcr < 0.06f && durationMs in 400..4000 -> EventType.POSSIBLE_SNORING to if (rhythmic) 0.75f else 0.5f
            avgZcr in 0.06f..0.2f && durationMs >= 1200 -> EventType.POSSIBLE_TALKING to 0.4f
            else -> EventType.ENVIRONMENTAL to 0.55f
        }
        onEvent(Detected(eventStartChunk * 100, durationMs, type, peak, conf, eventStartChunk, endChunk))
    }
}
