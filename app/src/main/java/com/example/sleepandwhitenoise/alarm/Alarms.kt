package com.example.sleepandwhitenoise.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sleepandwhitenoise.MainActivity
import com.example.sleepandwhitenoise.Notifications
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.data.AppSettings
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.Repeat
import java.util.Calendar

enum class ScheduleResult { SCHEDULED_EXACT, SCHEDULED_INEXACT, DISABLED }

object AlarmScheduler {
    private const val ACTION_ALARM = "com.example.sleepandwhitenoise.ALARM"
    private const val ACTION_REMINDER = "com.example.sleepandwhitenoise.REMINDER"
    const val ACTION_DISMISS = "com.example.sleepandwhitenoise.DISMISS_ALARM"

    fun canScheduleExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Next occurrence of [minOfDay] honoring [repeat]; returns epoch millis. */
    fun nextTrigger(minOfDay: Int, repeat: Repeat, now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minOfDay / 60); set(Calendar.MINUTE, minOfDay % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        if (repeat == Repeat.WEEKDAYS) {
            while (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return c.timeInMillis
    }

    fun scheduleAlarm(ctx: Context, s: AppSettings): ScheduleResult {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, ACTION_ALARM, 100)
        am.cancel(pi)
        if (!s.alarmEnabled) return ScheduleResult.DISABLED
        val at = nextTrigger(s.alarmMin, s.alarmRepeat)
        return if (canScheduleExact(ctx)) {
            val show = PendingIntent.getActivity(ctx, 101, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
            ScheduleResult.SCHEDULED_EXACT
        } else {
            // Without exact-alarm access the system may delay delivery; the UI tells the user so.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            ScheduleResult.SCHEDULED_INEXACT
        }
    }

    fun scheduleReminder(ctx: Context, s: AppSettings) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, ACTION_REMINDER, 200)
        am.cancel(pi)
        if (!s.reminderEnabled) return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(s.reminderMin, s.reminderRepeat), pi)
    }

    fun rescheduleAll(ctx: Context, s: AppSettings) {
        scheduleAlarm(ctx, s)
        scheduleReminder(ctx, s)
    }

    private fun pending(ctx: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun handle(ctx: Context, action: String?) {
        val app = SleepApp.get(ctx)
        val s = app.settings.current
        when (action) {
            ACTION_ALARM -> {
                postAlarm(ctx, s)
                if (s.alarmRepeat == Repeat.ONCE) app.settings.update { it.copy(alarmEnabled = false) }
                else scheduleAlarm(ctx, s)
            }
            ACTION_REMINDER -> {
                postReminder(ctx, s)
                if (s.reminderRepeat == Repeat.ONCE) app.settings.update { it.copy(reminderEnabled = false) }
                else scheduleReminder(ctx, s)
            }
            ACTION_DISMISS -> Notifications.manager(ctx).cancel(Notifications.ID_ALARM)
        }
    }

    private fun postAlarm(ctx: Context, s: AppSettings) {
        if (!Notifications.canPost(ctx)) return
        val open = PendingIntent.getActivity(ctx, 102, Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_TRACKING), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val dismiss = PendingIntent.getBroadcast(ctx, 103, Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, Notifications.alarmChannel(s.alarmTone, s.alarmVibrate))
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Good morning")
            .setContentText("It's ${Fmt.timeOfDay(s.alarmMin)}. Time to wake up gently.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Dismiss", dismiss)
            .build().apply { flags = flags or Notification.FLAG_INSISTENT }
        Notifications.manager(ctx).notify(Notifications.ID_ALARM, n)
    }

    private fun postReminder(ctx: Context, s: AppSettings) {
        if (!Notifications.canPost(ctx)) return
        val open = PendingIntent.getActivity(ctx, 104, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, Notifications.CH_REMINDER)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Bedtime is coming up")
            .setContentText("Your bedtime goal is ${Fmt.timeOfDay(s.bedtimeMin)}. Start winding down.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        Notifications.manager(ctx).notify(Notifications.ID_REMINDER, n)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        SleepApp.get(context).whenSettingsLoaded {
            AlarmScheduler.handle(context, intent.action)
            pending.finish()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = SleepApp.get(context)
        app.whenSettingsLoaded {
            AlarmScheduler.rescheduleAll(context, app.settings.current)
            pending.finish()
        }
    }
}
