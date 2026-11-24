package com.example.sleepandwhitenoise.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.sleepandwhitenoise.data.Mix
import com.example.sleepandwhitenoise.data.MixLayer
import com.example.sleepandwhitenoise.data.SoundCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val layers: List<MixLayer> = emptyList(),
    val title: String = "",
    val subtitle: String = "",
    val playing: Boolean = false,
    val volume: Float = 0.6f,
    val timerTotalMs: Long = 0,
    val timerRemainingMs: Long? = null,
    val fadeMs: Long = 0,
) {
    val hasContent: Boolean get() = layers.isNotEmpty()
    val primarySoundId: String? get() = layers.firstOrNull()?.soundId
}

class PlayerController(private val context: Context, private val scope: CoroutineScope) {
    private val engine = NoiseEngine()
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var timerJob: Job? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> engine.setMaster(_state.value.volume * 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> engine.setMaster(_state.value.volume)
        }
    }

    fun setDefaultVolume(v: Float) {
        if (!_state.value.playing) _state.update { it.copy(volume = v) }
    }

    fun playSound(id: String) {
        val s = SoundCatalog.require(id)
        load(listOf(MixLayer(id, 1f)), s.name, "${s.category.title} collection")
        play()
    }

    fun playMix(mix: Mix) {
        load(mix.layers, mix.name, "Custom mix • ${mix.layers.size} sounds")
        play()
    }

    /** Replace what's loaded without starting playback (used by the mixer). */
    fun load(layers: List<MixLayer>, title: String, subtitle: String) {
        _state.update { it.copy(layers = layers, title = title, subtitle = subtitle) }
        pushLayers()
    }

    fun addLayer(id: String) {
        if (_state.value.layers.any { it.soundId == id } || _state.value.layers.size >= MAX_LAYERS) return
        _state.update { st ->
            val layers = st.layers + MixLayer(id, 0.6f)
            st.copy(layers = layers, title = if (layers.size > 1) "Custom mix" else st.title, subtitle = "Custom mix • ${layers.size} sounds")
        }
        pushLayers()
    }

    fun removeLayer(id: String) {
        _state.update { st ->
            val layers = st.layers.filterNot { it.soundId == id }
            st.copy(layers = layers, subtitle = "Custom mix • ${layers.size} sounds")
        }
        pushLayers()
        if (_state.value.layers.isEmpty()) pause()
    }

    fun setLayerVolume(id: String, v: Float) {
        _state.update { st -> st.copy(layers = st.layers.map { if (it.soundId == id) it.copy(volume = v) else it }) }
        engine.setLayerVolume(id, v)
    }

    fun setVolume(v: Float) {
        _state.update { it.copy(volume = v) }
        if (_state.value.timerRemainingMs == null) engine.setMaster(v)
    }

    fun toggle() = if (_state.value.playing) pause() else play()

    fun play() {
        if (!_state.value.hasContent) return
        if (!requestFocus()) return
        pushLayers()
        engine.setMaster(_state.value.volume)
        engine.start()
        _state.update { it.copy(playing = true) }
        ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
    }

    fun pause() {
        engine.stop()
        abandonFocus()
        _state.update { it.copy(playing = false) }
    }

    fun stop() {
        pause()
        cancelTimer()
        _state.update { PlayerState(volume = it.volume) }
    }

    fun startTimer(minutes: Int, fadeMinutes: Int) {
        val total = minutes * 60_000L
        val fade = (fadeMinutes * 60_000L).coerceAtMost(total)
        val end = System.currentTimeMillis() + total
        timerJob?.cancel()
        _state.update { it.copy(timerTotalMs = total, timerRemainingMs = total, fadeMs = fade) }
        if (!_state.value.playing) play()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = end - System.currentTimeMillis()
                if (remaining <= 0) {
                    pause()
                    engine.setMaster(_state.value.volume)
                    _state.update { it.copy(timerRemainingMs = null, timerTotalMs = 0) }
                    break
                }
                if (fade > 0 && remaining < fade) engine.setMaster(_state.value.volume * remaining / fade.toFloat())
                _state.update { it.copy(timerRemainingMs = remaining) }
                delay(500)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        engine.setMaster(_state.value.volume)
        _state.update { it.copy(timerRemainingMs = null, timerTotalMs = 0) }
    }

    private fun pushLayers() {
        engine.setLayers(_state.value.layers.map { l ->
            l.soundId to (SoundCatalog.require(l.soundId).synth to l.volume)
        })
    }

    private fun requestFocus(): Boolean {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION") audioManager.abandonAudioFocus(focusListener)
        }
    }

    companion object { const val MAX_LAYERS = 5 }
}
