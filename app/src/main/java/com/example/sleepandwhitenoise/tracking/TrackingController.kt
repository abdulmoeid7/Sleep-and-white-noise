package com.example.sleepandwhitenoise.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.sleepandwhitenoise.data.SoundEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TrackingStatus { IDLE, RUNNING, PAUSED, FINISHING, FINISHED, ERROR }

data class TrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val sessionId: String? = null,
    val startedAt: Long = 0,
    /** Listening time accumulated before the current running stretch. */
    val listenedBeforeMs: Long = 0,
    val runningSince: Long = 0,
    val events: List<SoundEvent> = emptyList(),
    val levels: List<Float> = emptyList(),
    val noiseFloor: Float = 0f,
    val monitoring: Boolean = true,
    val error: String? = null,
    val lastFinishedSessionId: String? = null,
) {
    val active: Boolean get() = status == TrackingStatus.RUNNING || status == TrackingStatus.PAUSED
    fun elapsedMs(now: Long = System.currentTimeMillis()): Long = if (startedAt == 0L) 0 else now - startedAt
    fun listenedMs(now: Long = System.currentTimeMillis()): Long =
        listenedBeforeMs + if (status == TrackingStatus.RUNNING) now - runningSince else 0
}

/** UI-facing handle for the sleep tracking foreground service. */
class TrackingController(private val context: Context) {
    internal val mutableState = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = mutableState.asStateFlow()

    fun start(monitoring: Boolean, saveClips: Boolean) {
        mutableState.value = TrackingState(status = TrackingStatus.RUNNING, monitoring = monitoring)
        ContextCompat.startForegroundService(
            context,
            Intent(context, SleepTrackingService::class.java).setAction(SleepTrackingService.ACTION_START)
                .putExtra(SleepTrackingService.EXTRA_MONITORING, monitoring)
                .putExtra(SleepTrackingService.EXTRA_CLIPS, saveClips),
        )
    }

    fun pause() = send(SleepTrackingService.ACTION_PAUSE)
    fun resume() = send(SleepTrackingService.ACTION_RESUME)
    fun finish() = send(SleepTrackingService.ACTION_STOP)

    /** Clear a finished/error state once the UI has consumed it. */
    fun acknowledge() = mutableState.update {
        if (it.active || it.status == TrackingStatus.FINISHING) it
        else TrackingState(lastFinishedSessionId = it.lastFinishedSessionId)
    }

    private fun send(action: String) {
        context.startService(Intent(context, SleepTrackingService::class.java).setAction(action))
    }
}
