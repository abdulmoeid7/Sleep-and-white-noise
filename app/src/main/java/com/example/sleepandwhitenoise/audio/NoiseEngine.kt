package com.example.sleepandwhitenoise.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.sleepandwhitenoise.data.Synth
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Real-time stereo soundscape player. Each layer is an independent [Soundscape]; layers are summed
 * with smoothed volumes, passed through a transparent peak limiter and streamed to a float
 * [AudioTrack].
 */
class NoiseEngine {
    private class Layer(val id: String, synth: Synth, volume: Float) {
        val sound = Soundscape.create(synth)
        @Volatile var target = volume
        var current = 0f
    }

    private val layers = CopyOnWriteArrayList<Layer>()
    @Volatile private var masterTarget = 1f
    private var master = 0f
    @Volatile private var running = false
    private var thread: Thread? = null

    fun setLayers(spec: List<Pair<String, Pair<Synth, Float>>>) {
        val ids = spec.map { it.first }.toSet()
        layers.removeAll { it.id !in ids }
        spec.forEach { (id, sv) ->
            val existing = layers.firstOrNull { it.id == id }
            if (existing != null) existing.target = sv.second else layers += Layer(id, sv.first, sv.second)
        }
    }

    fun setLayerVolume(id: String, v: Float) {
        layers.firstOrNull { it.id == id }?.target = v
    }

    fun setMaster(v: Float) { masterTarget = v.coerceIn(0f, 1f) }

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "noise-engine").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        master = 0f
    }

    private fun loop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
        val mask = AudioFormat.CHANNEL_OUT_STEREO
        val minBuf = AudioTrack.getMinBufferSize(RATE, mask, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(mask).build()
            )
            // Generous buffer: background noise doesn't need low latency, and it prevents dropouts.
            .setBufferSizeInBytes(maxOf(minBuf * 2, FRAMES * 2 * 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val l = FloatArray(FRAMES); val r = FloatArray(FRAMES)
        val tl = FloatArray(FRAMES); val tr = FloatArray(FRAMES)
        val out = FloatArray(FRAMES * 2)
        track.play()
        try {
            while (running) {
                java.util.Arrays.fill(l, 0f); java.util.Arrays.fill(r, 0f)
                for (layer in layers) {
                    layer.sound.render(tl, tr, FRAMES)
                    for (i in 0 until FRAMES) {
                        layer.current += (layer.target - layer.current) * SMOOTH
                        l[i] += tl[i] * layer.current
                        r[i] += tr[i] * layer.current
                    }
                }
                for (i in 0 until FRAMES) {
                    master += (masterTarget - master) * SMOOTH
                    out[2 * i] = limit(l[i] * master)
                    out[2 * i + 1] = limit(r[i] * master)
                }
                track.write(out, 0, out.size, AudioTrack.WRITE_BLOCKING)
            }
        } finally {
            runCatching { track.pause(); track.flush(); track.stop() }
            track.release()
        }
    }

    companion object {
        const val RATE = 44_100
        private const val FRAMES = 1024
        private const val SMOOTH = 0.0005f
        private const val KNEE = 0.7f

        /** Linear below the knee (no coloring), smooth saturation above it so stacked layers never clip. */
        internal fun limit(x: Float): Float {
            val a = abs(x)
            if (a <= KNEE) return x
            val y = KNEE + (1f - KNEE) * tanh((a - KNEE) / (1f - KNEE))
            return if (x < 0) -y else y
        }
    }
}
