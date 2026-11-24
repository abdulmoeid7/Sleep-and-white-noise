package com.example.sleepandwhitenoise

import com.example.sleepandwhitenoise.alarm.AlarmScheduler
import com.example.sleepandwhitenoise.data.EventType
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.Repeat
import com.example.sleepandwhitenoise.data.SleepSession
import com.example.sleepandwhitenoise.data.SoundEvent
import com.example.sleepandwhitenoise.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LogicTest {
    private fun at(year: Int, month: Int, day: Int, h: Int, m: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month, day, h, m, 0)
    }.timeInMillis

    private fun cal(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }

    @Test
    fun nextTrigger_laterToday() {
        val now = at(2026, Calendar.NOVEMBER, 24, 5, 0) // Tuesday 05:00
        val c = cal(AlarmScheduler.nextTrigger(6 * 60 + 30, Repeat.DAILY, now))
        assertEquals(24, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
    }

    @Test
    fun nextTrigger_rollsToTomorrow() {
        val now = at(2026, Calendar.NOVEMBER, 24, 7, 0)
        val c = cal(AlarmScheduler.nextTrigger(6 * 60 + 30, Repeat.ONCE, now))
        assertEquals(25, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun nextTrigger_weekdaysSkipsWeekend() {
        val now = at(2026, Calendar.NOVEMBER, 27, 8, 0) // Friday after the alarm
        val c = cal(AlarmScheduler.nextTrigger(6 * 60 + 30, Repeat.WEEKDAYS, now))
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK))
        assertEquals(30, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun durationFormatting() {
        assertEquals("7h 42m", Fmt.duration((7 * 60 + 42) * 60_000L))
        assertEquals("8h 05m", Fmt.duration((8 * 60 + 5) * 60_000L))
        assertEquals("45m", Fmt.duration(45 * 60_000L))
        assertEquals("02:18:36", Fmt.clock((2 * 3600 + 18 * 60 + 36) * 1000L))
    }

    @Test
    fun periodStats_averagesAndConsistency() {
        val s1Start = at(2026, Calendar.NOVEMBER, 22, 22, 30)
        val s2Start = at(2026, Calendar.NOVEMBER, 23, 0, 45) // 2h15m after goal: inconsistent
        val sessions = listOf(
            SleepSession("a", s1Start, s1Start + 8 * 3_600_000L, 8 * 3_600_000L, 6 * 3_600_000L,
                listOf(SoundEvent(60_000, 2_000, EventType.POSSIBLE_SNORING, 50f, 0.6f))),
            SleepSession("b", s2Start, s2Start + 6 * 3_600_000L, 6 * 3_600_000L, 4 * 3_600_000L, emptyList()),
        )
        val st = Stats.forRange(sessions, 0, Long.MAX_VALUE, bedtimeGoalMin = 22 * 60 + 30)
        assertEquals(2, st.nights)
        assertEquals(7 * 3_600_000L, st.averageMs)
        assertEquals(1, st.consistentNights)
        assertEquals(1, st.totalEvents)
    }

    @Test
    fun eventsPerHour_bucketsEvents() {
        val start = at(2026, Calendar.NOVEMBER, 22, 23, 0)
        val s = SleepSession("x", start, start + 3 * 3_600_000L, 3 * 3_600_000L, 0, listOf(
            SoundEvent(10 * 60_000L, 1000, EventType.ENVIRONMENTAL, 40f, 0.5f),
            SoundEvent(70 * 60_000L, 1000, EventType.ENVIRONMENTAL, 40f, 0.5f),
            SoundEvent(80 * 60_000L, 1000, EventType.ENVIRONMENTAL, 40f, 0.5f),
        ))
        assertEquals(listOf(1, 2, 0), Stats.eventsPerHour(s))
        assertTrue(Stats.eventsPerHour(s).sum() == s.events.size)
    }
}
