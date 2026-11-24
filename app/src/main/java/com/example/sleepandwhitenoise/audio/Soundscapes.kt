package com.example.sleepandwhitenoise.audio

import com.example.sleepandwhitenoise.data.Synth
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * A stereo, procedurally generated soundscape. Left and right use independent noise sources so the
 * sound feels wide and natural on headphones. Every transient has a smoothed attack (no clicks),
 * and each sound is calibrated to roughly −20 dBFS RMS with peaks well below full scale.
 */
internal abstract class Soundscape(protected val fs: Float) {
    /** Calibration so every sound plays at a similar, comfortable loudness. */
    protected abstract val gain: Float

    /** Overwrites [l] and [r] with the next [n] frames. */
    fun render(l: FloatArray, r: FloatArray, n: Int) {
        for (i in 0 until n) {
            frame()
            l[i] = outL * gain
            r[i] = outR * gain
        }
    }

    protected var outL = 0f
    protected var outR = 0f
    protected abstract fun frame()

    companion object {
        fun create(synth: Synth, fs: Float = NoiseEngine.RATE.toFloat(), seed: Int = System.nanoTime().toInt()): Soundscape = when (synth) {
            Synth.WHITE -> WhiteNoise(fs, seed)
            Synth.PINK -> PinkNoise(fs, seed)
            Synth.BROWN -> BrownNoise(fs, seed)
            Synth.RAIN -> Rain(fs, seed)
            Synth.OCEAN -> Ocean(fs, seed)
            Synth.FOREST -> Forest(fs, seed)
            Synth.FIREPLACE -> Fireplace(fs, seed)
            Synth.WIND -> Wind(fs, seed)
        }
    }
}

/** Per-channel helpers so each soundscape can declare "two of" something concisely. */
private inline fun <T> pair(f: (Int) -> T): List<T> = listOf(f(0), f(1))

/** Short noise bursts (raindrops, leaves, crackles) with a smoothed attack and exponential decay. */
private class Grains(fs: Float, private val rng: Rng, decaySec: Float, attackSec: Float) {
    private val decay = exp(-1.0 / (decaySec * fs)).toFloat()
    private val attack = Smooth(attackSec, fs)
    private var env = 0f
    fun next(probability: Float, minAmp: Float, maxAmp: Float): Float {
        if (rng.chance(probability)) env += rng.range(minAmp, maxAmp)
        env *= decay
        return rng.white() * attack.next(env)
    }
}

// ---------------------------------------------------------------- Colored noise

/** White noise with the harsh top octave rolled off — still bright, no longer piercing. */
private class WhiteNoise(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.284f
    private val rng = pair { Rng(seed + it * 7919) }
    private val lp = pair { Biquad(fs).lowpass(8000f) }
    private val hp = pair { Biquad(fs).highpass(30f) }
    override fun frame() {
        outL = hp[0].process(lp[0].process(rng[0].white()))
        outR = hp[1].process(lp[1].process(rng[1].white()))
    }
}

private class PinkNoise(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.646f
    private val pink = pair { Pink(Rng(seed + it * 7919)) }
    private val lp = pair { Biquad(fs).lowpass(10000f) }
    private val hp = pair { Biquad(fs).highpass(25f) }
    override fun frame() {
        outL = hp[0].process(lp[0].process(pink[0].next()))
        outR = hp[1].process(lp[1].process(pink[1].next()))
    }
}

private class BrownNoise(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.358f
    private val brown = pair { Brown(Rng(seed + it * 7919)) }
    private val hp = pair { Biquad(fs).highpass(25f) }
    private val lp = pair { Biquad(fs).lowpass(1200f) }
    override fun frame() {
        outL = lp[0].process(hp[0].process(brown[0].next()))
        outR = lp[1].process(hp[1].process(brown[1].next()))
    }
}

// ---------------------------------------------------------------- Nature

/** Steady rainfall: a soft hiss bed, dense patter of tiny drops, and a distant low rumble. */
private class Rain(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.367f
    private val rng = pair { Rng(seed + it * 7919) }
    private val pink = pair { Pink(rng[it]) }
    private val brown = pair { Brown(rng[it]) }
    private val bedHp = pair { Biquad(fs).highpass(350f) }
    private val bedLp = pair { Biquad(fs).lowpass(5500f) }
    private val drops = pair { Grains(fs, rng[it], decaySec = 0.006f, attackSec = 0.0006f) }
    private val dropBp = pair { Biquad(fs).bandpass(2600f, 0.7f) }
    private val rumbleLp = pair { Biquad(fs).lowpass(220f) }
    private val intensity = Drift(Rng(seed xor 0x5f3759df), fs, 18f)

    override fun frame() {
        val k = 0.8f + 0.2f * intensity.next()
        val p = 160f * k / fs
        outL = voice(0, p) * k
        outR = voice(1, p) * k
    }

    private fun voice(c: Int, p: Float): Float {
        val bed = bedLp[c].process(bedHp[c].process(pink[c].next()))
        val patter = dropBp[c].process(drops[c].next(p, 0.15f, 0.9f))
        val rumble = rumbleLp[c].process(brown[c].next())
        return bed * 1.0f + patter * 0.55f + rumble * 0.7f
    }
}

/** Waves: a slow build, a soft crash, then a long wash, with the filter opening as each wave breaks. */
private class Ocean(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.515f
    private val rng = Rng(seed xor 0x2545F491)
    private val pink = pair { Pink(Rng(seed + it * 7919)) }
    private val brown = pair { Brown(Rng(seed + 31 + it * 7919)) }
    private val surfLp = pair { Biquad(fs).lowpass(400f, 0.5f) }
    private val bodyLp = pair { Biquad(fs).lowpass(320f) }
    private val foamHp = pair { Biquad(fs).highpass(2200f) }
    private val foamLp = pair { Biquad(fs).lowpass(7000f) }
    private val envS = pair { Smooth(0.08f, fs) }
    private var phase = 0f
    private var period = 9f
    private var tick = 0

    private fun waveShape(p: Float): Float =
        if (p < 0.45f) smoothstep(p / 0.45f).toDouble().pow(1.5).toFloat()
        else exp(-(p - 0.45f) * 4.5f)

    override fun frame() {
        phase += 1f / (period * fs)
        if (phase >= 1f) { phase -= 1f; period = rng.range(7f, 12f) }
        val e0 = envS[0].next(0.12f + 0.88f * waveShape(phase))
        val e1 = envS[1].next(0.12f + 0.88f * waveShape((phase + 0.035f) % 1f))
        if (tick++ % 32 == 0) {
            surfLp[0].lowpass(180f + 2200f * e0 * e0, 0.5f)
            surfLp[1].lowpass(180f + 2200f * e1 * e1, 0.5f)
        }
        outL = voice(0, e0)
        outR = voice(1, e1)
    }

    private fun voice(c: Int, e: Float): Float {
        val n = pink[c].next()
        val surf = surfLp[c].process(n)
        val body = bodyLp[c].process(brown[c].next()) * (0.35f + 0.65f * e)
        val foam = foamLp[c].process(foamHp[c].process(n)) * e * e * e
        return (surf * 1.0f + body * 0.9f + foam * 0.35f) * (0.3f + 0.7f * e)
    }
}

/** Wind: band-limited noise whose pitch and strength follow slow, irregular gusts. */
private class Wind(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.526f
    private val pink = pair { Pink(Rng(seed + it * 7919)) }
    private val brown = pair { Brown(Rng(seed + 17 + it * 7919)) }
    private val bp = pair { Biquad(fs).bandpass(500f, 1.2f) }
    private val lowLp = pair { Biquad(fs).lowpass(180f) }
    private val gust = Drift(Rng(seed xor 0x68E31DA4), fs, 6f)
    private val flutter = Drift(Rng(seed xor 0x1B56C4E9), fs, 1.5f)
    private var tick = 0

    override fun frame() {
        val g = 0.75f * gust.next() + 0.25f * flutter.next()
        if (tick++ % 32 == 0) {
            bp[0].bandpass(280f + 650f * g, 1.2f)
            bp[1].bandpass(300f + 620f * g, 1.2f)
        }
        val a = 0.25f + 0.75f * g
        outL = bp[0].process(pink[0].next()) * a + lowLp[0].process(brown[0].next()) * 0.5f
        outR = bp[1].process(pink[1].next()) * a + lowLp[1].process(brown[1].next()) * 0.5f
    }
}

/** Forest: a soft breeze through leaves, light rustling, and occasional distant birdsong. */
private class Forest(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 1.589f
    private val rng = pair { Rng(seed + it * 7919) }
    private val pink = pair { Pink(rng[it]) }
    private val breezeHp = pair { Biquad(fs).highpass(180f) }
    private val breezeLp = pair { Biquad(fs).lowpass(1600f) }
    private val rustle = pair { Grains(fs, rng[it], decaySec = 0.012f, attackSec = 0.002f) }
    private val rustleBp = pair { Biquad(fs).bandpass(3200f, 0.9f) }
    private val breeze = Drift(Rng(seed xor 0x7FEB352D), fs, 8f)
    private val birds = Birds(fs, Rng(seed xor 0x046CA68B))

    override fun frame() {
        val b = breeze.next()
        val amp = 0.45f + 0.55f * b
        val p = (6f + 30f * b) / fs
        birds.next()
        outL = voice(0, amp, p) + birds.l
        outR = voice(1, amp, p) + birds.r
    }

    private fun voice(c: Int, amp: Float, p: Float): Float {
        val air = breezeLp[c].process(breezeHp[c].process(pink[c].next())) * amp
        val leaves = rustleBp[c].process(rustle[c].next(p, 0.1f, 0.5f)) * 0.35f
        return air + leaves
    }
}

/**
 * Gentle birdsong: short phrases of 2–5 chirps at random intervals and positions. Each chirp is a
 * sine sweep with a Hann window, so it starts and ends smoothly.
 */
private class Birds(private val fs: Float, private val rng: Rng) {
    var l = 0f; private set
    var r = 0f; private set
    private var wait = (fs * 2).toInt()
    private var chirpsLeft = 0
    private var len = 0; private var t = 0
    private var f0 = 0f; private var f1 = 0f; private var amp = 0f; private var pan = 0.5f
    private var phase = 0.0

    fun next() {
        if (len == 0) {
            l = 0f; r = 0f
            if (--wait > 0) return
            if (chirpsLeft == 0) { chirpsLeft = 2 + (rng.uniform() * 4).toInt(); pan = rng.range(0.15f, 0.85f); amp = rng.range(0.03f, 0.07f) }
            len = (rng.range(0.06f, 0.14f) * fs).toInt(); t = 0
            f0 = rng.range(2600f, 4200f); f1 = f0 * rng.range(0.7f, 1.15f)
            chirpsLeft--
        }
        val x = t.toFloat() / len
        val f = f0 + (f1 - f0) * x + 40f * sin(2.0 * PI * 28.0 * t / fs).toFloat()
        phase += 2.0 * PI * f / fs
        val w = 0.5f - 0.5f * kotlin.math.cos(2.0 * PI * x).toFloat()
        val s = sin(phase).toFloat() * w * amp
        l = s * (1f - pan); r = s * pan
        if (++t >= len) {
            len = 0
            wait = if (chirpsLeft > 0) (rng.range(0.07f, 0.16f) * fs).toInt() else (rng.range(3f, 10f) * fs).toInt()
        }
    }
}

// ---------------------------------------------------------------- Cozy

/** Fireplace: a warm low roar that flickers, soft hiss, and sparse gentle crackles and pops. */
private class Fireplace(fs: Float, seed: Int) : Soundscape(fs) {
    override val gain = 0.200f
    private val rng = pair { Rng(seed + it * 7919) }
    private val pink = pair { Pink(rng[it]) }
    private val brown = pair { Brown(rng[it]) }
    private val roarLp = pair { Biquad(fs).lowpass(170f) }
    private val hissBp = pair { Biquad(fs).bandpass(1100f, 0.6f) }
    private val crackle = pair { Grains(fs, rng[it], decaySec = 0.0018f, attackSec = 0.00025f) }
    private val crackleHp = pair { Biquad(fs).highpass(1200f) }
    private val crackleLp = pair { Biquad(fs).lowpass(6000f) }
    private val pop = pair { Grains(fs, rng[it], decaySec = 0.009f, attackSec = 0.0008f) }
    private val popBp = pair { Biquad(fs).bandpass(550f, 1.4f) }
    private val flicker = Drift(Rng(seed xor 0x3C6EF372), fs, 1.2f)

    override fun frame() {
        val f = flicker.next()
        outL = voice(0, f)
        outR = voice(1, f)
    }

    private fun voice(c: Int, f: Float): Float {
        val roar = roarLp[c].process(brown[c].next()) * (0.7f + 0.3f * f)
        val hiss = hissBp[c].process(pink[c].next()) * 0.18f * (0.6f + 0.4f * f)
        val cr = crackleLp[c].process(crackleHp[c].process(crackle[c].next((3f + 6f * f) / fs, 0.05f, 0.5f))) * 0.6f
        val pp = popBp[c].process(pop[c].next(0.35f / fs, 0.2f, 0.6f)) * 0.8f
        return roar * 1.6f + hiss + cr + pp
    }
}
