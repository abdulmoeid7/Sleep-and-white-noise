package com.example.sleepandwhitenoise.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.Notifications
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.alarm.AlarmScheduler
import com.example.sleepandwhitenoise.alarm.ScheduleResult
import com.example.sleepandwhitenoise.data.AlarmTone
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.Repeat
import com.example.sleepandwhitenoise.data.SoundCatalog
import com.example.sleepandwhitenoise.tracking.Mic
import com.example.sleepandwhitenoise.tracking.TrackingStatus
import com.example.sleepandwhitenoise.ui.LocalSnackbar
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppCard
import com.example.sleepandwhitenoise.ui.components.AppChip
import com.example.sleepandwhitenoise.ui.components.ConfirmDialog
import com.example.sleepandwhitenoise.ui.components.HeroImage
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.MetricPanel
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.RadioRow
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.SecondaryButton
import com.example.sleepandwhitenoise.ui.components.SectionTitle
import com.example.sleepandwhitenoise.ui.components.StatTile
import com.example.sleepandwhitenoise.ui.components.StateContent
import com.example.sleepandwhitenoise.ui.components.SwitchRow
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.TimePickerDialog
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.components.Waveform
import com.example.sleepandwhitenoise.ui.resetTo
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun hasMic(ctx: android.content.Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

fun openAppSettings(ctx: android.content.Context) {
    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
}

/** Ticks every second while composed; used for live clocks. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (isActive) { now = System.currentTimeMillis(); delay(1000) } }
    return now
}

@Composable
fun SleepDashboardScreen(app: SleepApp, nav: NavHostController) {
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val tracking by app.tracking.state.collectAsStateWithLifecycle()
    val last = sessions.firstOrNull()
    ScreenScaffold(
        title = "Sleep", subtitle = "Build a more consistent sleep routine.",
        bottom = {
            PrimaryButton(if (tracking.active) "Return to sleep session" else "Start sleep tracking", {
                nav.navigate(if (tracking.active) Routes.TRACKING else Routes.SESSION_SETUP)
            })
        },
    ) {
        HeroImage(painterResource(R.drawable.bg_night_landscape), aspect = 1.9f) {
            Text("Tonight", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFFE6E8F5))
            Text("Bed by ${Fmt.timeOfDay(s.bedtimeMin)}", style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color.White)
            Text("Goal ${Fmt.hours(s.sleepTargetMin)}", style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color(0xFFE6E8F5))
        }
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            if (tracking.active) {
                val now = rememberNow()
                ListRow("Session in progress", value = Fmt.clock(tracking.elapsedMs(now)), icon = R.drawable.ic_mic,
                    onClick = { nav.navigate(Routes.TRACKING) })
            }
            if (last != null) {
                ListRow("Last session", value = Fmt.duration(last.durationMs), subtitle = Fmt.dayLong(last.startMs) + if (last.isExample) " • Example" else "",
                    icon = R.drawable.ic_sleep, onClick = { nav.navigate(Routes.report(last.id)) })
            } else {
                ListRow("Last session", value = "None yet", icon = R.drawable.ic_sleep, onClick = { nav.navigate(Routes.HISTORY) })
            }
            ListRow("Wake alarm", value = if (s.alarmEnabled) Fmt.timeOfDay(s.alarmMin) else "Off", icon = R.drawable.ic_alarm,
                onClick = { nav.navigate(Routes.ALARM) })
            ListRow("Sleep goals", value = Fmt.hours(s.sleepTargetMin), icon = R.drawable.ic_star, onClick = { nav.navigate(Routes.GOALS) })
            ListRow("Sleep history", icon = R.drawable.ic_insights, onClick = { nav.navigate(Routes.HISTORY) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(app: SleepApp, nav: NavHostController) {
    val ctx = LocalContext.current
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    var pickAudio by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        nav.navigate(if (ok) Routes.MIC_TEST else Routes.PERM_DENIED)
    }
    ScreenScaffold(
        title = "Sleep session setup", subtitle = "Prepare for a quiet night.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Continue", {
                when {
                    !s.soundMonitoring -> nav.navigate(Routes.PLACEMENT)
                    hasMic(ctx) -> nav.navigate(Routes.MIC_TEST)
                    else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Wake alarm", value = if (s.alarmEnabled) Fmt.timeOfDay(s.alarmMin) else "Off", icon = R.drawable.ic_alarm,
                onClick = { nav.navigate(Routes.ALARM) })
            SwitchRow("Sound monitoring", s.soundMonitoring, { v -> app.settings.update { it.copy(soundMonitoring = v) } },
                subtitle = "Uses the microphone overnight", icon = R.drawable.ic_mic)
            ListRow("Relaxation audio", value = s.relaxationSoundId?.let { SoundCatalog.get(it)?.name } ?: "None",
                subtitle = "Plays for 30 minutes, then fades", icon = R.drawable.ic_sounds, onClick = { pickAudio = true })
            SwitchRow("Save event clips", s.saveClips, { v -> app.settings.update { it.copy(saveClips = v) } },
                subtitle = "Short clips of detected sounds, kept only on this device", icon = R.drawable.ic_download,
                enabled = s.soundMonitoring)
        }
        VSpace(Space.l)
        InfoNote("Sound events are estimates based on loudness and pitch. This app does not measure sleep stages or diagnose any condition.")
    }
    if (pickAudio) {
        ModalBottomSheet(onDismissRequest = { pickAudio = false }, containerColor = c.background) {
            Column(Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.xl), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Text("Relaxation audio", style = MaterialTheme.typography.headlineMedium, color = c.text)
                VSpace(Space.s)
                RadioRow("None", s.relaxationSoundId == null, { app.settings.update { it.copy(relaxationSoundId = null) }; pickAudio = false })
                SoundCatalog.all.forEach { snd ->
                    RadioRow(snd.name, s.relaxationSoundId == snd.id, {
                        app.settings.update { it.copy(relaxationSoundId = snd.id) }; pickAudio = false
                    })
                }
            }
        }
    }
}

@Composable
fun WakeAlarmScreen(app: SleepApp, nav: NavHostController) {
    val ctx = LocalContext.current
    val c = AppTheme.colors
    val s = app.settings.current
    var enabled by rememberSaveable { mutableStateOf(s.alarmEnabled) }
    var time by rememberSaveable { mutableStateOf(s.alarmMin) }
    var repeat by rememberSaveable { mutableStateOf(s.alarmRepeat) }
    var tone by rememberSaveable { mutableStateOf(s.alarmTone) }
    var vibrate by rememberSaveable { mutableStateOf(s.alarmVibrate) }
    var picking by remember { mutableStateOf(false) }
    var exactOk by remember { mutableStateOf(AlarmScheduler.canScheduleExact(ctx)) }
    val notifOk = Notifications.canPost(ctx)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) exactOk = AlarmScheduler.canScheduleExact(ctx) }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    ScreenScaffold(
        title = "Wake-up alarm", subtitle = "Choose when to wake up.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save alarm", {
                app.settings.update { it.copy(alarmEnabled = enabled, alarmMin = time, alarmRepeat = repeat, alarmTone = tone, alarmVibrate = vibrate) }
                val r = AlarmScheduler.scheduleAlarm(ctx, app.settings.current)
                scope.launch {
                    snackbar.showSnackbar(when (r) {
                        ScheduleResult.SCHEDULED_EXACT -> "Alarm set for ${Fmt.timeOfDay(time)}"
                        ScheduleResult.SCHEDULED_INEXACT -> "Alarm saved, but it may be delayed without exact alarm access"
                        ScheduleResult.DISABLED -> "Alarm turned off"
                    })
                }
                nav.popBackStack()
            })
        },
    ) {
        AppCard(onClick = { picking = true }) {
            Text(
                Fmt.timeOfDay(time), style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 64.sp),
                color = if (enabled) c.primary else c.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl).semantics { contentDescription = "Alarm time ${Fmt.timeOfDay(time)}. Tap to change." },
            )
        }
        VSpace(Space.m)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            SwitchRow("Alarm", enabled, { enabled = it }, icon = R.drawable.ic_alarm)
            Text("Repeat", style = MaterialTheme.typography.titleMedium, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Repeat.entries.forEach { r -> AppChip(r.label, repeat == r, { repeat = r }) }
            }
            Text("Alarm sound", style = MaterialTheme.typography.titleMedium, color = c.text)
            AlarmTone.entries.forEach { t -> RadioRow(t.label, tone == t, { tone = t }) }
            SwitchRow("Vibration", vibrate, { vibrate = it }, icon = R.drawable.ic_vibrate)
        }
        if (enabled && !exactOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VSpace(Space.l)
            InfoNote("Exact alarms are restricted on this device, so Android may ring this alarm up to an hour late. Allow “Alarms & reminders” for on-time delivery.", color = c.warning)
            TextAction("Allow exact alarms", {
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
            })
        }
        if (enabled && !notifOk) {
            VSpace(Space.l)
            InfoNote("Notifications are off for this app, so the alarm can't ring. Turn them on in Android settings.", color = c.warning)
            TextAction("Open Android settings", { openAppSettings(ctx) })
        }
    }
    if (picking) TimePickerDialog("Wake time", time, { picking = false }) { time = it; picking = false }
}

@Composable
fun MicTestScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val c = AppTheme.colors
    val levels = remember { mutableStateOf(listOf<Float>()) }
    var heard by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var floor by remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(Unit) {
        if (!hasMic(ctx)) { failed = true; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            val rec = Mic.open() ?: run { failed = true; return@withContext }
            val buf = ShortArray(Mic.CHUNK)
            val window = ArrayDeque<Float>()
            try {
                rec.startRecording()
                while (isActive) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    val l = Mic.level(buf, n)
                    if (floor < 0) floor = l else if (l < floor) floor += (l - floor) * 0.2f else floor += (l - floor) * 0.01f
                    if (l > floor + 12f) heard = true
                    window.addLast(l); if (window.size > 48) window.removeFirst()
                    levels.value = window.toList()
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }
    }
    ScreenScaffold(
        title = "Microphone test", subtitle = "Check that your microphone can hear nearby sounds.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Continue", { nav.navigate(Routes.PLACEMENT) }) },
    ) {
        MetricPanel("Sound activity", when {
            failed -> "Unavailable"
            heard -> "We hear you"
            else -> "Listening…"
        }) {
            Waveform(levels.value, active = !failed)
        }
        VSpace(Space.l)
        Text(
            when {
                failed -> "The microphone couldn't be opened. Another app may be using it."
                heard -> "Great, nearby sounds are coming through clearly."
                else -> "Try a gentle cough or tap the table near your phone."
            },
            style = MaterialTheme.typography.bodyLarge, color = if (heard) c.success else c.muted,
        )
        VSpace(Space.l)
        InfoNote("This test isn't recorded or saved.")
    }
}

@Composable
fun PlacementScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    ScreenScaffold(
        title = "Phone placement", subtitle = "Place your phone on a stable bedside surface, uncovered.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Start session", {
                val s = app.settings.current
                s.relaxationSoundId?.let { id ->
                    app.player.playSound(id)
                    app.player.startTimer(30, s.fadeOutMin)
                }
                app.tracking.start(monitoring = s.soundMonitoring && hasMic(ctx), saveClips = s.saveClips)
                nav.navigate(Routes.TRACKING) { popUpTo(Routes.SESSION_SETUP) { inclusive = true } }
            })
        },
    ) {
        Image(painterResource(R.drawable.ill_phone_placement), null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(24.dp)))
        VSpace(Space.xl)
        listOf(
            "Within arm's reach, about 50 cm from your pillow.",
            "Microphone uncovered, not under a pillow or blanket.",
            "Plug in your charger for long sessions.",
            "Turn on Do Not Disturb if you like. Your alarm still rings.",
        ).forEach {
            Row(Modifier.padding(vertical = 6.dp)) {
                Text("•", color = c.primary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(Space.m))
                Text(it, color = c.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ActiveTrackingScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val st by app.tracking.state.collectAsStateWithLifecycle()
    var confirmEnd by remember { mutableStateOf(false) }
    val now = rememberNow()
    LaunchedEffect(st.status) {
        when (st.status) {
            TrackingStatus.ERROR -> nav.navigate(Routes.TRACKING_ERROR) { popUpTo(Routes.TRACKING) { inclusive = true } }
            TrackingStatus.FINISHING, TrackingStatus.FINISHED -> nav.navigate(Routes.ANALYZING) { popUpTo(Routes.TRACKING) { inclusive = true } }
            else -> Unit
        }
    }
    if (st.status == TrackingStatus.IDLE) {
        ScreenScaffold(title = "Sleep session", onBack = { nav.popBackStack() },
            bottom = { PrimaryButton("Set up a session", { nav.navigate(Routes.SESSION_SETUP) { popUpTo(Routes.TRACKING) { inclusive = true } } }) }) {
            StateContent(R.drawable.ill_empty, "No session running", "Start a sleep session when you're ready for bed.")
        }
        return
    }
    val paused = st.status == TrackingStatus.PAUSED
    ScreenScaffold(
        title = if (paused) "Tracking paused" else "Sleep in progress",
        subtitle = when {
            !st.monitoring -> "Session timer running. Sound monitoring is off."
            paused -> "Sound monitoring is paused."
            else -> "Monitoring sound activity on this device."
        },
        onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("End session", { confirmEnd = true }) },
    ) {
        MetricPanel(if (paused) "Paused" else "Elapsed", Fmt.clock(st.elapsedMs(now))) {
            if (st.monitoring) Waveform(st.levels, active = !paused)
            else Text("Started ${Fmt.time(st.startedAt)}", color = c.muted, style = MaterialTheme.typography.bodyLarge)
        }
        VSpace(Space.l)
        if (st.monitoring) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                StatTile("Sound events", st.events.size.toString(), Modifier.weight(1f))
                StatTile("Listening", Fmt.duration(st.listenedMs(now)), Modifier.weight(1f))
            }
            VSpace(Space.m)
            if (paused) PrimaryButton("Resume tracking", { app.tracking.resume() })
            else SecondaryButton("Pause tracking", { app.tracking.pause() })
            if (st.events.isNotEmpty()) {
                VSpace(Space.l)
                SectionTitle("Latest")
                st.events.takeLast(3).reversed().forEach { e ->
                    Text("${Fmt.time(st.startedAt + e.atMs)} • ${e.type.label}", color = c.muted, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
        VSpace(Space.l)
        InfoNote("You can leave this screen or lock your phone. The session keeps running and shows a notification.")
    }
    if (confirmEnd) {
        ConfirmDialog(
            "End session?", "Finish and save this sleep session?", "End and save", {
                confirmEnd = false
                app.tracking.finish()
            }, "Keep tracking", { confirmEnd = false },
        )
    }
}

@Composable
fun AnalyzingScreen(app: SleepApp, nav: NavHostController) {
    val st by app.tracking.state.collectAsStateWithLifecycle()
    var minDelayDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1800); minDelayDone = true }
    val id = st.lastFinishedSessionId
    val ready = minDelayDone && st.status != TrackingStatus.FINISHING && id != null
    LaunchedEffect(ready) {
        if (ready) {
            app.tracking.acknowledge()
            nav.navigate(Routes.summary(id!!)) { popUpTo(Routes.ANALYZING) { inclusive = true } }
        }
    }
    ScreenScaffold(
        title = null, onBack = null,
        bottom = { PrimaryButton("View when ready", { }, loading = !ready) },
    ) {
        StateContent(null, "Analyzing session", "Preparing your overnight sound summary.", loading = true, icon = R.drawable.ic_insights)
    }
}

@Composable
fun PermissionDeniedScreen(app: SleepApp, nav: NavHostController) {
    val ctx = LocalContext.current
    val c = AppTheme.colors
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && hasMic(ctx)) nav.navigate(Routes.MIC_TEST) { popUpTo(Routes.PERM_DENIED) { inclusive = true } }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    ScreenScaffold(
        title = null, onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Open Android settings", { openAppSettings(ctx) })
            TextAction("Continue without tracking", {
                val s = app.settings.current
                val id = s.relaxationSoundId
                if (id != null) {
                    app.player.playSound(id)
                    app.player.startTimer(30, s.fadeOutMin)
                    nav.navigate(Routes.PLAYER) { popUpTo(Routes.SESSION_SETUP) { inclusive = true } }
                } else nav.navigate(Routes.LIBRARY) { popUpTo(Routes.SESSION_SETUP) { inclusive = true } }
            }, Modifier.align(Alignment.CenterHorizontally), color = c.muted)
        },
    ) {
        StateContent(R.drawable.ill_error, "Microphone is off",
            "Microphone access is needed for sound monitoring. You can still play sounds and use the sleep timer without it.")
    }
}

@Composable
fun TrackingErrorScreen(app: SleepApp, nav: NavHostController) {
    val st by app.tracking.state.collectAsStateWithLifecycle()
    val msg = st.error ?: "We could not load this content. Your data is safe."
    ScreenScaffold(
        title = null, onBack = { app.tracking.acknowledge(); nav.popBackStack() },
        bottom = {
            PrimaryButton("Try again", { app.tracking.acknowledge(); nav.navigate(Routes.SESSION_SETUP) { popUpTo(Routes.TRACKING_ERROR) { inclusive = true } } })
            TextAction("Back to home", { app.tracking.acknowledge(); nav.resetTo(Routes.HOME) }, Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        StateContent(R.drawable.ill_error, "Something went wrong", msg)
    }
}
