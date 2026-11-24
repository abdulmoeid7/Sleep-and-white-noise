package com.example.sleepandwhitenoise.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Small, allocation-free DSP building blocks used by the soundscape generators. */

/** xorshift32 — fast uniform noise, one instance per channel so left/right are decorrelated. */
internal class Rng(seed: Int) {
    private var s = if (seed == 0) 0x1234567 else seed
    fun nextInt(): Int { var x = s; x = x xor (x shl 13); x = x xor (x ushr 17); x = x xor (x shl 5); s = x; return x }
    /** Uniform in [0, 1). */
    fun uniform(): Float = (nextInt() ushr 8) * (1f / 16_777_216f)
    /** Uniform in [-1, 1). */
    fun white(): Float = uniform() * 2f - 1f
    fun chance(p: Float): Boolean = uniform() < p
    fun range(a: Float, b: Float): Float = a + (b - a) * uniform()
}

/** Pink (1/f) noise, Paul Kellet's refined filter. RMS ≈ 0.2 for uniform white input. */
internal class Pink(private val rng: Rng) {
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f; private var b3 = 0f
    private var b4 = 0f; private var b5 = 0f; private var b6 = 0f
    fun next(): Float {
        val w = rng.white()
        b0 = 0.99886f * b0 + w * 0.0555179f; b1 = 0.99332f * b1 + w * 0.0750759f
        b2 = 0.96900f * b2 + w * 0.1538520f; b3 = 0.86650f * b3 + w * 0.3104856f
        b4 = 0.55000f * b4 + w * 0.5329522f; b5 = -0.7616f * b5 - w * 0.0168980f
        val p = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
        b6 = w * 0.115926f
        return p * 0.11f
    }
}

/** Brown (1/f²) noise: a leaky integrator, so it can't drift into DC or clip. */
internal class Brown(private val rng: Rng) {
    private var y = 0f
    fun next(): Float { y = y * 0.996f + rng.white() * 0.06f; return y }
}

/** RBJ-cookbook biquad in transposed direct form II. Coefficients can be retuned on the fly. */
internal class Biquad(private val fs: Float) {
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f; private var a1 = 0f; private var a2 = 0f
    private var z1 = 0f; private var z2 = 0f

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun lowpass(f: Float, q: Float = 0.707f) = apply { set(LP, f, q) }
    fun highpass(f: Float, q: Float = 0.707f) = apply { set(HP, f, q) }
    /** Band-pass with 0 dB peak gain. */
    fun bandpass(f: Float, q: Float) = apply { set(BP, f, q) }

    /** Allocation-free so it can be retuned every few samples on the audio thread. */
    private fun set(type: Int, f: Float, q: Float) {
        val w0 = 2.0 * PI * f.coerceIn(10f, fs * 0.45f) / fs
        val c = cos(w0).toFloat()
        val alpha = (sin(w0) / (2.0 * q)).toFloat()
        val n0: Float; val n1: Float; val n2: Float
        when (type) {
            LP -> { n0 = (1 - c) / 2; n1 = 1 - c; n2 = (1 - c) / 2 }
            HP -> { n0 = (1 + c) / 2; n1 = -(1 + c); n2 = (1 + c) / 2 }
            else -> { n0 = alpha; n1 = 0f; n2 = -alpha }
        }
        val a0 = 1f + alpha
        b0 = n0 / a0; b1 = n1 / a0; b2 = n2 / a0
        a1 = -2f * c / a0; a2 = (1f - alpha) / a0
    }

    private companion object { const val LP = 0; const val HP = 1; const val BP = 2 }
}

/** One-pole smoother: removes steps from envelopes and control signals (no clicks). */
internal class Smooth(timeSec: Float, fs: Float) {
    private val k = (1.0 - exp(-1.0 / (timeSec * fs))).toFloat()
    var value = 0f
    fun next(target: Float): Float { value += (target - value) * k; return value }
}

/**
 * Slowly wandering control signal in 0..1 (gusts, flicker, intensity). A smoothed random walk
 * that changes direction every [periodSec] on average.
 */
internal class Drift(private val rng: Rng, private val fs: Float, private val periodSec: Float) {
    private var target = rng.uniform()
    private val s = Smooth(periodSec / 2f, fs).also { it.value = target }
    private var left = 0
    fun next(): Float {
        if (--left <= 0) { target = rng.uniform(); left = (rng.range(0.5f, 1.5f) * periodSec * fs).toInt() }
        return s.next(target)
    }
}

internal fun smoothstep(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3 - 2 * t) }
