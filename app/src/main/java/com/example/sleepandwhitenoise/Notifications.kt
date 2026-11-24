package com.example.sleepandwhitenoise

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.sleepandwhitenoise.data.AlarmTone

object Notifications {
    const val CH_PLAYBACK = "playback"
    const val CH_TRACKING = "tracking"
    const val CH_REMINDER = "reminders"
    const val ID_PLAYBACK = 1
    const val ID_TRACKING = 2
    const val ID_ALARM = 3
    const val ID_REMINDER = 4

    /** Channel sound/vibration can't change after creation, so each combination gets its own channel. */
    fun alarmChannel(tone: AlarmTone, vibrate: Boolean) = "alarm_${tone.name.lowercase()}_${if (vibrate) "vib" else "novib"}"

    fun manager(ctx: Context): NotificationManager = ctx.getSystemService(NotificationManager::class.java)

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = manager(ctx)
        nm.createNotificationChannel(NotificationChannel(CH_PLAYBACK, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Controls for sounds that are playing"; setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_TRACKING, "Sleep session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shown while a sleep session is using the microphone"; setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Bedtime reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A gentle nudge before your bedtime"
        })
        for (tone in AlarmTone.entries) for (vib in listOf(true, false)) {
            val uri = RingtoneManager.getDefaultUri(
                if (tone == AlarmTone.DEVICE_ALARM) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
            )
            nm.createNotificationChannel(
                NotificationChannel(alarmChannel(tone, vib), "Wake-up alarm • ${tone.label}${if (vib) "" else " (no vibration)"}",
                    NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(uri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    enableVibration(vib)
                    if (vib) vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
                    setBypassDnd(false)
                }
            )
        }
    }
}
