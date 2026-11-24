package com.example.sleepandwhitenoise.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.data.EventType
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.PeriodStats
import com.example.sleepandwhitenoise.data.SessionStore
import com.example.sleepandwhitenoise.data.SleepSession
import com.example.sleepandwhitenoise.data.Stats
import com.example.sleepandwhitenoise.ui.LocalSnackbar
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppCard
import com.example.sleepandwhitenoise.ui.components.BarChart
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.MetricPanel
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.SecondaryButton
import com.example.sleepandwhitenoise.ui.components.SectionTitle
import com.example.sleepandwhitenoise.ui.components.StatTile
import com.example.sleepandwhitenoise.ui.components.StateContent
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.resetTo
import com.example.sleepandwhitenoise.ui.switchTab
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlinx.coroutines.launch
import java.io.File

private const val ESTIMATE_NOTE = "Estimated from sound only. This is not a medical assessment and does not detect sleep stages."

/** Empty state shared by insights and history (design: "No sleep sessions yet"). */
@Composable
private fun NoSessions(app: SleepApp, nav: NavHostController) {
    StateContent(R.drawable.ill_empty, "No sleep sessions yet", "Record a night to see your duration, quiet time and sound events here.")
    PrimaryButton("Start a session", { nav.switchTab(Routes.SLEEP) })
    TextAction("Explore with example data", { app.sessions.loadExampleData() }, Modifier.fillMaxWidth())
}

@Composable
private fun SessionMissing(nav: NavHostController) {
    ScreenScaffold(title = null, onBack = { nav.popBackStack() }) {
        StateContent(R.drawable.ill_error, "Session not found", "This session may have been deleted. Your other data is safe.")
    }
}

@Composable
private fun TileRow(vararg tiles: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        tiles.forEach { (l, v) -> StatTile(l, v, Modifier.weight(1f)) }
    }
}

/** Normalizes durations to 0..1 for the bar chart, scaled to at least the sleep target. */
private fun normalize(values: List<Long?>, targetMin: Int): List<Float?> {
    val max = maxOf(values.filterNotNull().maxOrNull() ?: 0L, targetMin * 60_000L).toFloat()
    return values.map { v -> v?.let { it / max } }
}

@Composable
fun MorningSummaryScreen(app: SleepApp, nav: NavHostController, id: String) {
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val s = sessions.firstOrNull { it.id == id } ?: return SessionMissing(nav)
    val perHour = Stats.eventsPerHour(s)
    val max = (perHour.maxOrNull() ?: 0).coerceAtLeast(1)
    ScreenScaffold(
        title = if (Fmt.greeting() == "Good morning") "Good morning" else "Session saved",
        subtitle = "Your night at a glance.", onBack = { nav.resetTo(Routes.HOME) },
        bottom = {
            PrimaryButton("View full report", { nav.navigate(Routes.report(id)) })
            TextAction("Done", { nav.resetTo(Routes.HOME) }, Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        MetricPanel("Session duration", Fmt.duration(s.durationMs)) {
            if (s.trackedMs > 0) {
                BarChart(perHour.map { it / max.toFloat() }, perHour.indices.map { Fmt.time24(s.startMs + it * 3_600_000L).take(2) }, highlightLast = false, height = 96.dp)
                VSpace(Space.s)
                Text("Sound events per hour", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
            } else {
                Text("${Fmt.time(s.startMs)} – ${Fmt.time(s.endMs)}", style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.muted)
            }
        }
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Session duration", value = Fmt.duration(s.durationMs))
            if (s.trackedMs > 0) {
                ListRow("Sound events", value = s.events.size.toString())
                ListRow("Quiet time", value = Fmt.duration(s.quietMs))
            } else {
                ListRow("Sound monitoring", value = "Off")
            }
        }
        VSpace(Space.l)
        InfoNote(ESTIMATE_NOTE)
    }
}

@Composable
fun InsightsScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    if (sessions.isEmpty()) {
        ScreenScaffold(title = "Insights", subtitle = "Discover your sleep routine over time.") { NoSessions(app, nav) }
        return
    }
    val week = Stats.lastDays(sessions, 7, settings.bedtimeMin)
    val nightly = Stats.nightly(sessions, 7)
    ScreenScaffold(
        title = "Insights", subtitle = "Discover your sleep routine over time.",
        bottom = { PrimaryButton("View history", { nav.navigate(Routes.HISTORY) }) },
    ) {
        MetricPanel("Last 7 nights • average", if (week.nights > 0) Fmt.duration(week.averageMs) else "—") {
            BarChart(normalize(nightly.map { it.second }, settings.sleepTargetMin), nightly.map { it.first })
        }
        VSpace(Space.l)
        TileRow(
            "Average session" to (if (week.nights > 0) Fmt.duration(week.averageMs) else "—"),
            "Bedtime consistency" to "${week.consistentNights} of ${week.nights.coerceAtLeast(1)}",
        )
        VSpace(Space.s)
        Text("Consistency counts nights started within 45 minutes of your ${Fmt.timeOfDay(settings.bedtimeMin)} bedtime goal.",
            style = MaterialTheme.typography.bodySmall, color = c.muted)
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Weekly report", icon = R.drawable.ic_insights, onClick = { nav.navigate(Routes.WEEKLY) })
            ListRow("Monthly report", icon = R.drawable.ic_insights, onClick = { nav.navigate(Routes.MONTHLY) })
            ListRow("Sleep goals", value = Fmt.hours(settings.sleepTargetMin), icon = R.drawable.ic_star, onClick = { nav.navigate(Routes.GOALS) })
        }
        if (sessions.any { it.isExample }) {
            VSpace(Space.l)
            InfoNote("Includes example data. Remove it any time in Settings → Privacy and data.")
        }
    }
}

@Composable
fun DailyReportScreen(app: SleepApp, nav: NavHostController, id: String) {
    val c = AppTheme.colors
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val s = sessions.firstOrNull { it.id == id } ?: return SessionMissing(nav)
    ScreenScaffold(
        title = "Sleep report",
        subtitle = Fmt.dayLong(s.startMs) + if (s.isExample) " • Example session" else "",
        onBack = { nav.popBackStack() },
        bottom = { if (s.trackedMs > 0) PrimaryButton("View sound events", { nav.navigate(Routes.timeline(id)) }) },
    ) {
        TileRow("Session duration" to Fmt.duration(s.durationMs), "Quiet time" to if (s.trackedMs > 0) Fmt.duration(s.quietMs) else "—")
        VSpace(Space.m)
        TileRow("In bed" to Fmt.time(s.startMs), "Up" to Fmt.time(s.endMs))
        if (s.trackedMs > 0) {
            VSpace(Space.xl)
            SectionTitle("Sound events • ${s.events.size}")
            EventBreakdown(s)
            VSpace(Space.xl)
            SectionTitle("Through the night")
            val perHour = Stats.eventsPerHour(s)
            val max = (perHour.maxOrNull() ?: 0).coerceAtLeast(1)
            AppCard {
                Column(Modifier.padding(Space.l)) {
                    BarChart(perHour.map { it / max.toFloat() }, perHour.indices.map { Fmt.time24(s.startMs + it * 3_600_000L).take(2) }, highlightLast = false, height = 110.dp)
                    VSpace(Space.s)
                    Text("Events per hour, by clock hour", style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
        } else {
            VSpace(Space.l)
            InfoNote("Sound monitoring was off for this session, so only the duration was recorded.")
        }
        VSpace(Space.l)
        InfoNote(ESTIMATE_NOTE)
    }
}

@Composable
private fun EventBreakdown(s: SleepSession) {
    val c = AppTheme.colors
    val total = s.events.size.coerceAtLeast(1)
    AppCard {
        Column(Modifier.padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            EventType.entries.forEach { t ->
                val n = s.events.count { it.type == t }
                Column {
                    Row {
                        Text(t.label, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                        Text(n.toString(), style = MaterialTheme.typography.bodyLarge, color = c.muted)
                    }
                    VSpace(6.dp)
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.elevated)) {
                        Box(Modifier.fillMaxWidth(n / total.toFloat()).fillMaxHeight().clip(CircleShape).background(c.primary))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodReport(
    app: SleepApp, nav: NavHostController, title: String, subtitle: String, stats: PeriodStats,
    chart: List<Pair<String, Long?>>, showLabels: Boolean, cta: String, onCta: () -> Unit,
) {
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val hasExample = stats.sessions.any { it.isExample }
    ScreenScaffold(
        title = title, subtitle = subtitle + if (hasExample) " • Example data" else "", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton(cta, onCta) },
    ) {
        if (stats.nights == 0) {
            StateContent(R.drawable.ill_empty, "Nothing recorded yet", "Sessions you record in this period will appear here.")
            return@ScreenScaffold
        }
        MetricPanel("Average session", Fmt.duration(stats.averageMs)) {
            BarChart(normalize(chart.map { it.second }, settings.sleepTargetMin), if (showLabels) chart.map { it.first } else emptyList(), highlightLast = false)
        }
        VSpace(Space.l)
        TileRow("Sessions" to stats.nights.toString(), "Avg quiet time" to Fmt.duration(stats.averageQuietMs))
        VSpace(Space.m)
        TileRow("Sound events" to stats.totalEvents.toString(), "On-time bedtimes" to "${stats.consistentNights} of ${stats.nights}")
        VSpace(Space.l)
        InfoNote(ESTIMATE_NOTE)
    }
}

@Composable
fun WeeklyReportScreen(app: SleepApp, nav: NavHostController) {
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val bed = app.settings.current.bedtimeMin
    val now = System.currentTimeMillis()
    PeriodReport(
        app, nav, "Weekly report", "${Fmt.dayShort(now - 7 * SessionStore.DAY_MS)} – ${Fmt.dayShort(now)}",
        Stats.lastDays(sessions, 7, bed), Stats.nightly(sessions, 7), true, "View monthly report",
    ) { nav.navigate(Routes.MONTHLY) }
}

@Composable
fun MonthlyReportScreen(app: SleepApp, nav: NavHostController) {
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val bed = app.settings.current.bedtimeMin
    PeriodReport(
        app, nav, "Monthly report", Fmt.month(System.currentTimeMillis()),
        Stats.currentMonth(sessions, bed), Stats.nightly(sessions, 30), false, "View history",
    ) { nav.navigate(Routes.HISTORY) }
}

@Composable
fun HistoryScreen(app: SleepApp, nav: NavHostController) {
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "Sleep history", subtitle = "Your previous sleep sessions.", onBack = { nav.popBackStack() },
        bottom = if (sessions.isNotEmpty()) ({ PrimaryButton("Open latest session", { nav.navigate(Routes.report(sessions.first().id)) }) }) else null,
    ) {
        if (sessions.isEmpty()) { NoSessions(app, nav); return@ScreenScaffold }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            sessions.forEach { s ->
                ListRow(
                    Fmt.dayShort(s.startMs), value = Fmt.duration(s.durationMs),
                    subtitle = "${Fmt.time(s.startMs)} – ${Fmt.time(s.endMs)}" +
                        (if (s.trackedMs > 0) " • ${s.events.size} events" else "") + if (s.isExample) " • Example" else "",
                    onClick = { nav.navigate(Routes.report(s.id)) },
                )
            }
        }
    }
}

@Composable
fun TimelineScreen(app: SleepApp, nav: NavHostController, id: String) {
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val s = sessions.firstOrNull { it.id == id } ?: return SessionMissing(nav)
    ScreenScaffold(
        title = "Sound event timeline", subtitle = "Estimated sound events, not a diagnosis.", onBack = { nav.popBackStack() },
        bottom = if (s.events.isNotEmpty()) ({ PrimaryButton("Open event", { nav.navigate(Routes.event(id, 0)) }) }) else null,
    ) {
        if (s.events.isEmpty()) {
            StateContent(null, "A quiet night", "No sound events rose clearly above the background noise.", success = true)
            return@ScreenScaffold
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            s.events.forEachIndexed { i, e ->
                ListRow(
                    "${Fmt.time24(s.startMs + e.atMs)} • ${e.type.label}",
                    subtitle = "${e.durationMs / 1000}s • ${confidenceLabel(e.confidence)} confidence" + if (e.clipPath != null) " • Clip saved" else "",
                    icon = if (e.clipPath != null) R.drawable.ic_play else R.drawable.ic_sounds,
                    onClick = { nav.navigate(Routes.event(id, i)) },
                )
            }
        }
    }
}

private fun confidenceLabel(c: Float) = when {
    c >= 0.7f -> "Higher"
    c >= 0.5f -> "Medium"
    else -> "Low"
}

@Composable
fun EventDetailsScreen(app: SleepApp, nav: NavHostController, id: String, index: Int) {
    val c = AppTheme.colors
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val s = sessions.firstOrNull { it.id == id } ?: return SessionMissing(nav)
    val e = s.events.getOrNull(index) ?: return SessionMissing(nav)
    val clip = e.clipPath?.let { File(it) }?.takeIf { it.exists() }
    var playing by remember { mutableStateOf(false) }
    val mp = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { mp.value?.release() } }
    ScreenScaffold(
        title = e.type.label, subtitle = Fmt.time(s.startMs + e.atMs) + " • " + Fmt.dayShort(s.startMs),
        onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Back to timeline", { nav.popBackStack() }) },
    ) {
        MetricPanel("Relative loudness", "${e.peakLevel.toInt()}") {
            Text("On a 0–100 scale relative to the microphone's range", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Duration", value = "${e.durationMs / 1000.0}s".replace(".0s", "s"))
            ListRow("Confidence", value = confidenceLabel(e.confidence))
            if (clip != null) {
                ListRow(
                    if (playing) "Playing clip…" else "Play saved clip", icon = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    subtitle = "Stored only on this device",
                    onClick = {
                        if (playing) { mp.value?.stop(); mp.value?.release(); mp.value = null; playing = false }
                        else {
                            mp.value = MediaPlayer().apply {
                                setDataSource(clip.absolutePath); prepare(); start()
                                setOnCompletionListener { playing = false }
                            }
                            playing = true
                        }
                    },
                )
            } else {
                ListRow("Audio clip not saved", subtitle = "Turn on “Save event clips” in sleep tracking settings to keep short clips.",
                    icon = R.drawable.ic_shield)
            }
        }
        VSpace(Space.l)
        InfoNote("“${e.type.label}” is an estimate based on loudness, pitch and rhythm. It may be wrong and is not a diagnosis.")
        if (index < s.events.lastIndex) {
            VSpace(Space.s)
            TextAction("Next event", { nav.navigate(Routes.event(id, index + 1)) { popUpTo(Routes.EVENT) { inclusive = true } } })
        }
    }
}

@Composable
fun SleepGoalsScreen(app: SleepApp, nav: NavHostController) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    ScreenScaffold(
        title = "Sleep goals", subtitle = "Set a realistic nightly routine.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save goals", {
                scope.launch { snackbar.showSnackbar("Goals saved") }
                nav.popBackStack()
            })
        },
    ) {
        GoalEditor(app)
        VSpace(Space.l)
        InfoNote("Most adults need 7–9 hours. A steady bedtime matters as much as the total.")
    }
}
