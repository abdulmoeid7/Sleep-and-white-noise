package com.example.sleepandwhitenoise.tracking

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.sleepandwhitenoise.MainActivity
import com.example.sleepandwhitenoise.Notifications
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.data.SleepSession
import com.example.sleepandwhitenoise.data.SoundEvent
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * User-initiated overnight session. Runs as a microphone foreground service with a visible
 * notification. Audio is analysed in memory; nothing is stored unless the user opted into clips.
 */
class SleepTrackingService : Service() {
    private val app get() = SleepApp.get(this)
    private val ctrl get() = app.tracking
    @Volatile private var running = false
    @Volatile private var paused = false
    private var worker: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var saveClips = false
    private var monitoring = true
    private val events = mutableListOf<SoundEvent>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent.getBooleanExtra(EXTRA_MONITORING, true), intent.getBooleanExtra(EXTRA_CLIPS, false))
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> finish()
            else -> if (!running) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(monitoring: Boolean, clips: Boolean) {
        if (running) return
        this.monitoring = monitoring
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && monitoring && micGranted)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        val ok = runCatching { ServiceCompat.startForeground(this, Notifications.ID_TRACKING, notification(false), type) }.isSuccess
        if (!ok) { fail("Sleep tracking could not start. Open the app and try again."); return }
        if (monitoring && !micGranted) { fail("Microphone access is needed for sound monitoring."); return }

        saveClips = clips
        running = true
        paused = false
        events.clear()
        val now = System.currentTimeMillis()
        val id = now.toString()
        ctrl.mutableState.value = TrackingState(
            status = TrackingStatus.RUNNING, sessionId = id, startedAt = now, runningSince = now, monitoring = monitoring,
        )
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sleepandwhitenoise:tracking")
            .apply { acquire(14 * 60 * 60 * 1000L) }
        if (monitoring) worker = Thread({ listen(id) }, "sleep-tracking").apply { start() }
    }

    private fun listen(sessionId: String) {
        val rec = Mic.open()
        if (rec == null) { fail("The microphone is busy or unavailable."); return }
        val buf = ShortArray(Mic.CHUNK)
        val ringChunks = 150
        val ring = ShortArray(ringChunks * Mic.CHUNK)
        var chunk = 0L
        var clipCount = 0
        val levels = ArrayDeque<Float>()
        var recording = false
        val detector = EventDetector { d ->
            val now = System.currentTimeMillis()
            val st = ctrl.state.value
            val atMs = (now - st.startedAt) - (d.endChunk - d.startChunk) * 100
            var clipPath: String? = null
            if (saveClips && clipCount < MAX_CLIPS) {
                val from = maxOf(d.startChunk - 10, chunk - ringChunks + 1)
                val to = minOf(d.endChunk, from + 100)
                if (to > from) {
                    val n = ((to - from) * Mic.CHUNK).toInt()
                    val pcm = ShortArray(n)
                    for (c in from until to) {
                        val src = ((c % ringChunks) * Mic.CHUNK).toInt()
                        System.arraycopy(ring, src, pcm, ((c - from) * Mic.CHUNK).toInt(), Mic.CHUNK)
                    }
                    val f = File(app.sessions.clipsDir, "${sessionId}_${clipCount++}.wav")
                    runCatching { Mic.writeWav(f, pcm, n) }.onSuccess { clipPath = f.absolutePath }
                }
            }
            val e = SoundEvent(atMs.coerceAtLeast(0), d.durationMs, d.type, d.peak, d.confidence, clipPath)
            synchronized(events) { events += e }
            ctrl.mutableState.update { it.copy(events = it.events + e) }
        }
        try {
            while (running) {
                if (paused) {
                    if (recording) { rec.stop(); recording = false; detector.flush(chunk) }
                    Thread.sleep(200)
                    continue
                }
                if (!recording) { rec.startRecording(); recording = true }
                var read = 0
                while (read < Mic.CHUNK && running && !paused) {
                    val r = rec.read(buf, read, Mic.CHUNK - read)
                    if (r < 0) throw IllegalStateException("read failed $r")
                    read += r
                }
                if (read < Mic.CHUNK) continue
                System.arraycopy(buf, 0, ring, ((chunk % ringChunks) * Mic.CHUNK).toInt(), Mic.CHUNK)
                val level = Mic.level(buf, read)
                detector.feed(level, Mic.zcr(buf, read), chunk)
                chunk++
                levels.addLast(level)
                if (levels.size > 48) levels.removeFirst()
                if (chunk % 2 == 0L) ctrl.mutableState.update { it.copy(levels = levels.toList(), noiseFloor = detector.noiseFloor) }
            }
            detector.flush(chunk)
        } catch (t: Throwable) {
            if (running) fail("Sound monitoring stopped unexpectedly. Your session so far is safe.")
        } finally {
            runCatching { if (recording) rec.stop() }
            rec.release()
        }
    }

    private fun setPaused(p: Boolean) {
        if (!running || paused == p) return
        paused = p
        val now = System.currentTimeMillis()
        ctrl.mutableState.update {
            if (p) it.copy(status = TrackingStatus.PAUSED, listenedBeforeMs = it.listenedMs(now), levels = emptyList())
            else it.copy(status = TrackingStatus.RUNNING, runningSince = now)
        }
        Notifications.manager(this).notify(Notifications.ID_TRACKING, notification(p))
    }

    private fun finish() {
        if (!running) { stopSelf(); return }
        val now = System.currentTimeMillis()
        val st = ctrl.state.value
        ctrl.mutableState.update { it.copy(status = TrackingStatus.FINISHING) }
        running = false
        worker?.join(1500)
        val listened = st.listenedMs(now)
        val evs = synchronized(events) { events.sortedBy { it.atMs } }
        // Quiet time = listening time minus every minute that contained a sound event.
        val noisyMinutes = evs.flatMap { e -> (e.atMs / 60_000..(e.atMs + e.durationMs) / 60_000) }.toSet().size
        val quiet = if (monitoring) (listened - noisyMinutes * 60_000L).coerceAtLeast(0) else 0L
        val session = SleepSession(
            id = st.sessionId ?: now.toString(), startMs = st.startedAt, endMs = now,
            trackedMs = if (monitoring) listened else 0L, quietMs = quiet, events = evs,
        )
        app.sessions.add(session)
        ctrl.mutableState.value = TrackingState(status = TrackingStatus.FINISHED, lastFinishedSessionId = session.id)
        cleanup()
    }

    private fun fail(msg: String) {
        running = false
        ctrl.mutableState.update { it.copy(status = TrackingStatus.ERROR, error = msg) }
        cleanup()
    }

    private fun cleanup() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(paused: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 10, Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_TRACKING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, SleepTrackingService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when {
            !monitoring -> "Session in progress • microphone off"
            paused -> "Sound monitoring is paused"
            else -> "Monitoring sound activity on this device"
        }
        return NotificationCompat.Builder(this, Notifications.CH_TRACKING)
            .setSmallIcon(R.drawable.ic_sleep)
            .setContentTitle("Sleep session")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setWhen(ctrl.state.value.startedAt.takeIf { it > 0 } ?: System.currentTimeMillis())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .apply {
                if (monitoring) {
                    if (paused) addAction(R.drawable.ic_play, "Resume", action(ACTION_RESUME, 11))
                    else addAction(R.drawable.ic_pause, "Pause", action(ACTION_PAUSE, 12))
                }
            }
            .build()
    }

    override fun onDestroy() {
        running = false
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"
        const val EXTRA_MONITORING = "monitoring"
        const val EXTRA_CLIPS = "clips"
        private const val MAX_CLIPS = 30
    }
}
