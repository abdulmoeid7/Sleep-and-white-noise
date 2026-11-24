package com.example.sleepandwhitenoise.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.sleepandwhitenoise.MainActivity
import com.example.sleepandwhitenoise.Notifications
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps generated audio playing with the screen off, with a visible media notification. */
class PlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player get() = SleepApp.get(this).player
    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> player.toggle()
            ACTION_STOP -> player.stop()
        }
        val st = player.state.value
        ServiceCompat.startForeground(
            this, Notifications.ID_PLAYBACK, build(st),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        if (!st.hasContent) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        collectJob?.cancel()
        collectJob = scope.launch {
            player.state.collect { s ->
                if (!s.hasContent) {
                    stopSelfCleanly()
                } else {
                    Notifications.manager(this@PlaybackService).notify(Notifications.ID_PLAYBACK, build(s))
                    if (!s.playing) ServiceCompat.stopForeground(this@PlaybackService, ServiceCompat.STOP_FOREGROUND_DETACH)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stopSelfCleanly() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun build(s: PlayerState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, PlaybackService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE,
        )
        val timer = s.timerRemainingMs?.let { " • Timer ${com.example.sleepandwhitenoise.data.Fmt.minutesLeft(it)}" } ?: ""
        return NotificationCompat.Builder(this, Notifications.CH_PLAYBACK)
            .setSmallIcon(R.drawable.ic_sounds)
            .setContentTitle(s.title.ifBlank { "Sleep & White Noise" })
            .setContentText((if (s.playing) "Playing" else "Paused") + timer)
            .setContentIntent(open)
            .setOngoing(s.playing)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(if (s.playing) R.drawable.ic_pause else R.drawable.ic_play, if (s.playing) "Pause" else "Play", action(ACTION_TOGGLE, 1))
            .addAction(R.drawable.ic_close, "Stop", action(ACTION_STOP, 2))
            .setDeleteIntent(action(ACTION_STOP, 3))
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_STOP = "stop"
    }
}
