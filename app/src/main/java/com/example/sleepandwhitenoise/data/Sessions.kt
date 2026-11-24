package com.example.sleepandwhitenoise.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import kotlin.random.Random

/** Estimated, not diagnostic. Labels intentionally say "possible". */
enum class EventType(val label: String) {
    POSSIBLE_SNORING("Possible snoring"),
    POSSIBLE_TALKING("Possible talking"),
    ENVIRONMENTAL("Environmental noise"),
}

data class SoundEvent(
    val atMs: Long,
    val durationMs: Long,
    val type: EventType,
    val peakLevel: Float,
    /** 0..1 — how confident the on-device heuristic is in [type]. */
    val confidence: Float,
    val clipPath: String? = null,
)

data class SleepSession(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    /** Time the microphone was actually listening (excludes pauses). */
    val trackedMs: Long,
    val quietMs: Long,
    val events: List<SoundEvent>,
    val isExample: Boolean = false,
) {
    val durationMs: Long get() = endMs - startMs
}

/** Sleep sessions live only in this app's private storage (excluded from cloud backup). */
class SessionStore(private val context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "sessions.json")
    val clipsDir: File get() = File(context.filesDir, "clips").apply { mkdirs() }
    private val _sessions = MutableStateFlow<List<SleepSession>>(emptyList())
    /** Newest first. */
    val sessions: StateFlow<List<SleepSession>> = _sessions.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val mutex = Mutex()

    init {
        scope.launch(Dispatchers.IO) {
            _sessions.value = runCatching { if (file.exists()) decode(file.readText()) else emptyList() }
                .getOrDefault(emptyList()).sortedByDescending { it.startMs }
            _loaded.value = true
        }
    }

    fun get(id: String): SleepSession? = _sessions.value.firstOrNull { it.id == id }

    fun add(session: SleepSession) = mutate { (listOf(session) + it).sortedByDescending { s -> s.startMs } }

    fun deleteAll() {
        mutate { emptyList() }
        scope.launch(Dispatchers.IO) { clipsDir.listFiles()?.forEach { it.delete() } }
    }

    fun pruneOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - days * DAY_MS
        val old = _sessions.value.filter { it.startMs < cutoff && !it.isExample }
        if (old.isEmpty()) return
        old.flatMap { it.events }.mapNotNull { it.clipPath }.forEach { File(it).delete() }
        mutate { list -> list.filterNot { it in old } }
    }

    fun loadExampleData() = mutate { current -> (current + exampleSessions()).sortedByDescending { it.startMs } }

    private fun mutate(transform: (List<SleepSession>) -> List<SleepSession>) {
        val next = transform(_sessions.value)
        _sessions.value = next
        scope.launch(Dispatchers.IO) { mutex.withLock { file.writeText(encode(next)) } }
    }

    fun exportCsv(): String = buildString {
        appendLine("session_id,start,end,duration_min,quiet_min,event_time,event_type,event_duration_s,confidence,example")
        _sessions.value.forEach { s ->
            val base = "${s.id},${Fmt.isoDateTime(s.startMs)},${Fmt.isoDateTime(s.endMs)},${s.durationMs / 60000},${s.quietMs / 60000}"
            if (s.events.isEmpty()) appendLine("$base,,,,,${s.isExample}")
            s.events.forEach { e ->
                appendLine("$base,${Fmt.isoDateTime(s.startMs + e.atMs)},${e.type.label},${e.durationMs / 1000},${"%.2f".format(e.confidence)},${s.isExample}")
            }
        }
    }

    private fun encode(list: List<SleepSession>): String = JSONArray().apply {
        list.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id); put("start", s.startMs); put("end", s.endMs)
                put("tracked", s.trackedMs); put("quiet", s.quietMs); put("example", s.isExample)
                put("events", JSONArray().apply {
                    s.events.forEach { e ->
                        put(JSONObject().apply {
                            put("at", e.atMs); put("dur", e.durationMs); put("type", e.type.name)
                            put("peak", e.peakLevel.toDouble()); put("conf", e.confidence.toDouble())
                            put("clip", e.clipPath ?: JSONObject.NULL)
                        })
                    }
                })
            })
        }
    }.toString()

    private fun decode(raw: String): List<SleepSession> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val ev = o.getJSONArray("events")
            SleepSession(
                id = o.getString("id"), startMs = o.getLong("start"), endMs = o.getLong("end"),
                trackedMs = o.optLong("tracked", o.getLong("end") - o.getLong("start")),
                quietMs = o.getLong("quiet"), isExample = o.optBoolean("example"),
                events = List(ev.length()) { j ->
                    val e = ev.getJSONObject(j)
                    SoundEvent(
                        atMs = e.getLong("at"), durationMs = e.getLong("dur"),
                        type = runCatching { EventType.valueOf(e.getString("type")) }.getOrDefault(EventType.ENVIRONMENTAL),
                        peakLevel = e.getDouble("peak").toFloat(), confidence = e.optDouble("conf", 0.5).toFloat(),
                        clipPath = if (e.isNull("clip")) null else e.getString("clip"),
                    )
                },
            )
        }
    }

    /** Mock nights clearly flagged as examples (design: "Example metrics are mock data"). */
    private fun exampleSessions(): List<SleepSession> {
        val rnd = Random(42)
        val durations = intArrayOf(462, 432, 485, 410, 455, 398, 470, 440, 425, 480, 415, 450, 437, 466)
        return durations.mapIndexed { i, mins ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(i + 1))
                set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 20 + rnd.nextInt(40))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val dur = mins * 60_000L
            val events = (0 until 6 + rnd.nextInt(9)).map {
                val type = when (rnd.nextInt(10)) { in 0..5 -> EventType.POSSIBLE_SNORING; in 6..7 -> EventType.ENVIRONMENTAL; else -> EventType.POSSIBLE_TALKING }
                SoundEvent(
                    atMs = (rnd.nextDouble(0.05, 0.95) * dur).toLong(), durationMs = 1_000L + rnd.nextLong(9_000),
                    type = type, peakLevel = 45f + rnd.nextFloat() * 25f, confidence = 0.45f + rnd.nextFloat() * 0.4f,
                )
            }.sortedBy { it.atMs }
            SleepSession(
                id = "example-$i", startMs = start, endMs = start + dur, trackedMs = dur,
                quietMs = (dur * (0.62 + rnd.nextDouble() * 0.15)).toLong(), events = events, isExample = true,
            )
        }
    }

    companion object { const val DAY_MS = 24 * 60 * 60 * 1000L }
}

/** Aggregates for insights and reports. */
data class PeriodStats(
    val sessions: List<SleepSession>,
    val averageMs: Long,
    val averageQuietMs: Long,
    val totalEvents: Int,
    val consistentNights: Int,
    val nights: Int,
)

object Stats {
    fun forRange(all: List<SleepSession>, fromMs: Long, toMs: Long, bedtimeGoalMin: Int): PeriodStats {
        val list = all.filter { it.startMs in fromMs until toMs }
        val avg = if (list.isEmpty()) 0L else list.sumOf { it.durationMs } / list.size
        val avgQuiet = if (list.isEmpty()) 0L else list.sumOf { it.quietMs } / list.size
        val consistent = list.count { s ->
            val c = Calendar.getInstance().apply { timeInMillis = s.startMs }
            val minOfDay = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
            var diff = kotlin.math.abs(minOfDay - bedtimeGoalMin)
            if (diff > 720) diff = 1440 - diff
            diff <= 45
        }
        return PeriodStats(list, avg, avgQuiet, list.sumOf { it.events.size }, consistent, list.size)
    }

    fun lastDays(all: List<SleepSession>, days: Int, bedtimeGoalMin: Int): PeriodStats {
        val now = System.currentTimeMillis()
        return forRange(all, now - days * SessionStore.DAY_MS, now + 1, bedtimeGoalMin)
    }

    fun currentMonth(all: List<SleepSession>, bedtimeGoalMin: Int): PeriodStats {
        val c = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return forRange(all, c.timeInMillis, Long.MAX_VALUE, bedtimeGoalMin)
    }

    /** Duration per night for the last [days] nights, oldest first; null where no session. */
    fun nightly(all: List<SleepSession>, days: Int): List<Pair<String, Long?>> {
        val out = mutableListOf<Pair<String, Long?>>()
        val day = Calendar.getInstance()
        for (i in days downTo 1) {
            val c = (day.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -i); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
            }
            val from = c.timeInMillis
            val to = from + SessionStore.DAY_MS
            val s = all.filter { it.startMs in from until to }.maxByOrNull { it.durationMs }
            out += Fmt.weekdayShort(from) to s?.durationMs
        }
        return out
    }

    /** Hourly event counts across a session, for the daily report chart. */
    fun eventsPerHour(s: SleepSession): List<Int> {
        val hours = ((s.durationMs + 3_599_999) / 3_600_000).toInt().coerceIn(1, 14)
        val counts = IntArray(hours)
        s.events.forEach { e -> counts[(e.atMs / 3_600_000).toInt().coerceIn(0, hours - 1)]++ }
        return counts.toList()
    }
}
