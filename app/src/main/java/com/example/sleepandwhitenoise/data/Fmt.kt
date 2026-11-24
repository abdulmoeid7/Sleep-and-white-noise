package com.example.sleepandwhitenoise.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Formatting helpers (java.time needs desugaring on minSdk 24, so Calendar is used). */
object Fmt {
    fun duration(ms: Long): String {
        val totalMin = (ms / 60_000).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0L -> "${m}m"
            else -> "${h}h ${"%02d".format(m)}m"
        }
    }

    fun clock(ms: Long, withSeconds: Boolean = true): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return if (withSeconds) "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
        else "%02d:%02d".format(s / 3600, (s % 3600) / 60)
    }

    fun minutesLeft(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    /** Minutes since midnight → "10:30 PM". */
    fun timeOfDay(minOfDay: Int): String {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minOfDay / 60); set(Calendar.MINUTE, minOfDay % 60)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time)
    }

    fun time(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    fun time24(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    fun dayLong(ms: Long): String = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(ms))
    fun dayShort(ms: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    fun weekdayShort(ms: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(ms))
    fun month(ms: Long): String = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(ms))
    fun isoDateTime(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date(ms))
    fun fileStamp(ms: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))

    fun hours(min: Int): String = if (min % 60 == 0) "${min / 60} hours" else "${min / 60}h ${min % 60}m"

    fun greeting(nowMs: Long = System.currentTimeMillis()): String {
        val h = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else -> "Good night"
        }
    }
}
