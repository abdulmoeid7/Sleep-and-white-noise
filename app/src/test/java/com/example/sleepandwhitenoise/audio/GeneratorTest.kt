package com.example.sleepandwhitenoise.audio

import com.example.sleepandwhitenoise.data.Synth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Every soundscape must be comfortably audible, consistent in loudness and never clip.
 * Also writes 30 s stereo WAV previews to app/build/sound-previews for listening on a computer.
 */
class GeneratorTest {
    @Test
    fun soundscapesAreCalibratedAndClean() {
        val fs = 44_100
        val seconds = 30
        val outDir = File("build/sound-previews").apply { mkdirs() }
        val problems = mutableListOf<String>()
        for (synth in Synth.entries) {
            val s = Soundscape.create(synth, fs.toFloat(), seed = 12345)
            val n = fs * seconds
            val l = FloatArray(n); val r = FloatArray(n)
            s.render(l, r, n)
            var sum = 0.0; var peak = 0f; var corr = 0.0
            for (i in 0 until n) {
                sum += (l[i] * l[i] + r[i] * r[i]) / 2.0
                peak = maxOf(peak, abs(l[i]), abs(r[i]))
                corr += l[i] * r[i]
            }
            val rms = sqrt(sum / n)
            val db = 20 * log10(rms)
            println("%-10s rms=%.1f dBFS  peak=%.2f  L/R corr=%.2f".format(synth, db, peak, corr / sum))
            if (db !in -25.0..-15.0) problems += "$synth loudness $db dBFS out of range"
            if (peak >= 0.7f) problems += "$synth peak $peak would hit the limiter"
            writeWav(File(outDir, "${synth.name.lowercase()}.wav"), l, r, fs)
        }
        println("previews: ${outDir.absolutePath}")
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun writeWav(f: File, l: FloatArray, r: FloatArray, fs: Int) {
        val n = l.size
        val data = ByteBuffer.allocate(n * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) {
            data.putShort((l[i].coerceIn(-1f, 1f) * 32767).toInt().toShort())
            data.putShort((r[i].coerceIn(-1f, 1f) * 32767).toInt().toShort())
        }
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + n * 4); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(2); putInt(fs); putInt(fs * 4); putShort(4); putShort(16)
            put("data".toByteArray()); putInt(n * 4)
        }
        FileOutputStream(f).use { it.write(h.array()); it.write(data.array()) }
    }
}
