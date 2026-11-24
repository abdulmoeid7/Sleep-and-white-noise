package com.example.sleepandwhitenoise.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.Notifications
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.alarm.AlarmScheduler
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.Repeat
import com.example.sleepandwhitenoise.data.ThemeMode
import com.example.sleepandwhitenoise.ui.LocalSnackbar
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppCard
import com.example.sleepandwhitenoise.ui.components.AppChip
import com.example.sleepandwhitenoise.ui.components.AppIcon
import com.example.sleepandwhitenoise.ui.components.AppSlider
import com.example.sleepandwhitenoise.ui.components.AppTextField
import com.example.sleepandwhitenoise.ui.components.ConfirmDialog
import com.example.sleepandwhitenoise.ui.components.HeroImage
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.RadioRow
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.SecondaryButton
import com.example.sleepandwhitenoise.ui.components.SwitchRow
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.TimePickerDialog
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun rememberSaved(nav: NavHostController): (String) -> Unit {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    return { msg -> scope.launch { snackbar.showSnackbar(msg) }; nav.popBackStack() }
}

@Composable
fun ProfileScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "Profile", subtitle = "Your sleep space.",
        bottom = { PrimaryButton("Open settings", { nav.navigate(Routes.SETTINGS) }) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ill_avatar), "Profile picture", Modifier.size(72.dp).clip(CircleShape))
            Spacer(Modifier.width(Space.l))
            Column {
                Text(if (s.displayName.isBlank()) "Guest profile" else s.displayName, style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text(s.accountEmail ?: "Data stays on this device", style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
        }
        VSpace(Space.xl)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Edit profile", icon = R.drawable.ic_edit, onClick = { nav.navigate(Routes.EDIT_PROFILE) })
            if (s.signedIn) ListRow("Sign out", icon = R.drawable.ic_logout, onClick = { app.auth.signOut() })
            else ListRow("Sign in", subtitle = "Optional", icon = R.drawable.ic_profile, onClick = { nav.navigate(Routes.LOGIN) })
            ListRow("My mixes", value = s.mixes.size.toString(), icon = R.drawable.ic_mix, onClick = { nav.navigate(Routes.MIXES) })
            ListRow("Favorites", value = s.favorites.size.toString(), icon = R.drawable.ic_heart, onClick = { nav.navigate(Routes.FAVORITES) })
            ListRow("Premium", icon = R.drawable.ic_star, onClick = { nav.navigate(Routes.PREMIUM) })
        }
    }
}

@Composable
fun EditProfileScreen(app: SleepApp, nav: NavHostController) {
    var name by rememberSaveable { mutableStateOf(app.settings.current.displayName) }
    val done = rememberSaved(nav)
    ScreenScaffold(
        title = "Edit profile", subtitle = "Personalize your local profile.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Save changes", { app.settings.update { it.copy(displayName = name.trim()) }; done("Profile updated") }) },
    ) {
        Image(painterResource(R.drawable.ill_avatar), null, Modifier.size(112.dp).clip(CircleShape).align(Alignment.CenterHorizontally))
        VSpace(Space.xl)
        AppTextField(name, { name = it.take(30) }, "Display name", leadingIcon = R.drawable.ic_profile, imeAction = ImeAction.Done)
        VSpace(Space.s)
        Text("Used for your greeting on Home. Leave empty to stay a guest.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
    }
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    ScreenScaffold(
        title = "Settings", subtitle = "Make the app work for you.", onBack = { nav.popBackStack() },
        bottom = { SecondaryButton("Back to profile", { nav.popBackStack() }) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Audio settings", icon = R.drawable.ic_volume, onClick = { nav.navigate(Routes.AUDIO_SETTINGS) })
            ListRow("Sleep tracking settings", icon = R.drawable.ic_mic, onClick = { nav.navigate(Routes.TRACKING_SETTINGS) })
            ListRow("Reminder settings", icon = R.drawable.ic_bell, onClick = { nav.navigate(Routes.REMINDER_SETTINGS) })
            ListRow("Appearance", icon = R.drawable.ic_palette, onClick = { nav.navigate(Routes.APPEARANCE) })
            ListRow("Privacy and data", icon = R.drawable.ic_shield, onClick = { nav.navigate(Routes.PRIVACY) })
            ListRow("Help and FAQ", icon = R.drawable.ic_help, onClick = { nav.navigate(Routes.FAQ) })
        }
        VSpace(Space.l)
        Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun AudioSettingsScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s = app.settings.current
    var volume by rememberSaveable { mutableFloatStateOf(s.defaultVolume) }
    var fade by rememberSaveable { mutableIntStateOf(s.fadeOutMin) }
    var background by rememberSaveable { mutableStateOf(s.backgroundPlayback) }
    val done = rememberSaved(nav)
    ScreenScaffold(
        title = "Audio settings", subtitle = "Adjust your listening experience.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save preferences", {
                app.settings.update { it.copy(defaultVolume = volume, fadeOutMin = fade, backgroundPlayback = background) }
                app.player.setDefaultVolume(volume)
                done("Preferences saved")
            })
        },
    ) {
        AppCard {
            Column(Modifier.padding(Space.l)) {
                Row {
                    Text("Default volume", style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                    Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge, color = c.primary)
                }
                AppSlider(volume, { volume = it }, label = "Default volume")
            }
        }
        VSpace(Space.l)
        Text("Fade out", style = MaterialTheme.typography.titleMedium, color = c.text)
        Text("Used by the sleep timer and relaxation audio.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        VSpace(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(0, 1, 5, 10).forEach { f -> AppChip(if (f == 0) "Off" else "$f min", fade == f, { fade = f }) }
        }
        VSpace(Space.l)
        SwitchRow("Background playback", background, { background = it }, subtitle = "Keep playing when you leave the app or lock the screen")
    }
}

@Composable
fun TrackingSettingsScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    val s = app.settings.current
    var monitoring by rememberSaveable { mutableStateOf(s.soundMonitoring) }
    var clips by rememberSaveable { mutableStateOf(s.saveClips) }
    var retention by rememberSaveable { mutableIntStateOf(s.retentionDays) }
    val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val done = rememberSaved(nav)
    ScreenScaffold(
        title = "Sleep tracking settings", subtitle = "Control overnight monitoring.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save preferences", {
                app.settings.update { it.copy(soundMonitoring = monitoring, saveClips = clips, retentionDays = retention) }
                app.sessions.pruneOlderThan(retention)
                done("Preferences saved")
            })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            SwitchRow("Microphone monitoring", monitoring, { monitoring = it }, subtitle = "Detect sound events during sessions")
            SwitchRow("Save event clips", clips, { clips = it }, enabled = monitoring,
                subtitle = "Keep up to 10 s of audio around each event, on this device only")
            ListRow("Microphone access", value = if (micGranted) "Allowed" else "Not allowed", onClick = { openAppSettings(ctx) })
        }
        VSpace(Space.l)
        Text("Keep sessions for", style = MaterialTheme.typography.titleMedium, color = c.text)
        VSpace(Space.s)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(7, 30, 90, 365).forEach { d -> AppChip(if (d == 365) "1 year" else "$d days", retention == d, { retention = d }) }
        }
        VSpace(Space.l)
        InfoNote("Older sessions and their clips are deleted automatically.")
    }
}

@Composable
fun ReminderSettingsScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    val s = app.settings.current
    var enabled by rememberSaveable { mutableStateOf(s.reminderEnabled) }
    var time by rememberSaveable { mutableIntStateOf(s.reminderMin) }
    var repeat by rememberSaveable { mutableStateOf(s.reminderRepeat) }
    var picking by remember { mutableStateOf(false) }
    val done = rememberSaved(nav)
    ScreenScaffold(
        title = "Reminder settings", subtitle = "Build your bedtime routine.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save reminders", {
                app.settings.update { it.copy(reminderEnabled = enabled, reminderMin = time, reminderRepeat = repeat) }
                AlarmScheduler.scheduleReminder(ctx, app.settings.current)
                done(if (enabled) "Reminder set for ${Fmt.timeOfDay(time)}" else "Reminders off")
            })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            SwitchRow("Bedtime reminder", enabled, { enabled = it }, icon = R.drawable.ic_bell)
            ListRow("Reminder time", value = Fmt.timeOfDay(time), icon = R.drawable.ic_timer, onClick = { picking = true })
            Text("Repeat", style = MaterialTheme.typography.titleMedium, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Repeat.entries.forEach { r -> AppChip(r.label, repeat == r, { repeat = r }) }
            }
        }
        if (enabled && !Notifications.canPost(ctx)) {
            VSpace(Space.l)
            InfoNote("Notifications are off for this app, so reminders can't appear.", color = c.warning)
            TextAction("Open Android settings", { openAppSettings(ctx) })
        }
        VSpace(Space.l)
        InfoNote("Reminders may arrive a few minutes late when your phone is saving battery.")
    }
    if (picking) TimePickerDialog("Reminder time", time, { picking = false }) { time = it; picking = false }
}

@Composable
fun AppearanceScreen(app: SleepApp, nav: NavHostController) {
    val s = app.settings.current
    var theme by rememberSaveable { mutableStateOf(s.theme) }
    var reduce by rememberSaveable { mutableStateOf(s.reduceMotion) }
    val done = rememberSaved(nav)
    ScreenScaffold(
        title = "Appearance", subtitle = "Choose your preferred look.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Apply appearance", {
                app.settings.update { it.copy(theme = theme, reduceMotion = reduce) }
                done("Appearance updated")
            })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            RadioRow(ThemeMode.MIDNIGHT.label, theme == ThemeMode.MIDNIGHT, { theme = ThemeMode.MIDNIGHT }, subtitle = "Easy on the eyes at night")
            RadioRow(ThemeMode.SYSTEM.label, theme == ThemeMode.SYSTEM, { theme = ThemeMode.SYSTEM }, subtitle = "Follow your device's light or dark setting")
            SwitchRow("Reduce motion", reduce, { reduce = it }, subtitle = "Stop animated waveforms")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(app.sessions.exportCsv().toByteArray()) } }.isSuccess
            }
            snackbar.showSnackbar(if (ok) "Sleep history exported" else "Export failed. Your data is unchanged.")
        }
    }
    ScreenScaffold(
        title = "Privacy and data", subtitle = "You control your local information.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Manage deletion", { confirmDelete = true }, enabled = sessions.isNotEmpty()) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ListRow("Microphone access", value = if (micGranted) "Allowed" else "Not allowed", icon = R.drawable.ic_mic, onClick = { openAppSettings(ctx) })
            ListRow("Export sleep history", subtitle = "${sessions.size} sessions as CSV", icon = R.drawable.ic_download,
                onClick = { if (sessions.isEmpty()) scope.launch { snackbar.showSnackbar("No sessions to export yet") } else exporter.launch("sleep-history-${Fmt.fileStamp(System.currentTimeMillis())}.csv") })
            ListRow("Delete sleep data", icon = R.drawable.ic_trash, titleColor = c.error, onClick = { confirmDelete = true })
            ListRow("Privacy policy", icon = R.drawable.ic_shield, onClick = { showPolicy = true })
        }
        VSpace(Space.l)
        InfoNote("Sleep sessions and clips are stored only in this app's private storage and are excluded from cloud backup.")
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete data?", "Permanently delete local sleep history and saved clips? This can't be undone.",
            "Delete data", {
                app.sessions.deleteAll(); confirmDelete = false
                scope.launch { snackbar.showSnackbar("Sleep data deleted") }
            }, "Cancel", { confirmDelete = false }, destructive = true,
        )
    }
    if (showPolicy) {
        ModalBottomSheet(onDismissRequest = { showPolicy = false }, containerColor = c.background) {
            Column(Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.xl), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text("Privacy policy", style = MaterialTheme.typography.headlineMedium, color = c.text)
                listOf(
                    "Microphone audio is analyzed in memory on this device during sessions you start. It is never uploaded.",
                    "Audio clips are saved only if you turn on “Save event clips”, and are deleted with your sleep data or when they pass your retention period.",
                    "Sleep sessions, mixes and settings live in this app's private storage. Sessions and clips are excluded from cloud backup.",
                    "Optional accounts are not connected to a server in this version.",
                    "Exported files go only where you choose to save them.",
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge, color = c.muted) }
            }
        }
    }
}

private val faqs = listOf(
    "How does monitoring work?" to "During a session you start, the app listens through your microphone and notes moments that are clearly louder than the room's background noise. It estimates whether each moment sounds like possible snoring, talking or environmental noise. These are estimates, not a diagnosis, and the app does not measure sleep stages.",
    "Why is microphone access needed?" to "Only for sound monitoring during sleep sessions. Audio is processed on your device. You can play sounds, mix and use timers without granting it.",
    "Can I listen offline?" to "Yes. Every sound is generated on your device in real time, so nothing needs downloading and everything works in airplane mode.",
    "Will my alarm ring if the app is closed?" to "Yes, when exact alarms are allowed and notifications are on. Some battery savers can still delay alarms; the alarm screen tells you if something needs attention.",
    "How do I delete my data?" to "Go to Settings → Privacy and data → Delete sleep data. This removes all sessions and saved clips from this device.",
)

@Composable
fun FaqScreen(nav: NavHostController) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    var open by remember { mutableIntStateOf(-1) }
    ScreenScaffold(
        title = "Help and FAQ", subtitle = "Find answers to common questions.", onBack = { nav.popBackStack() },
        bottom = { SecondaryButton("Back to settings", { nav.popBackStack() }) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            faqs.forEachIndexed { i, (q, a) ->
                AppCard(Modifier.animateContentSize(), onClick = { open = if (open == i) -1 else i }) {
                    Column(Modifier.padding(Space.l)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(q, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                            AppIcon(if (open == i) R.drawable.ic_minus else R.drawable.ic_plus, if (open == i) "Collapse" else "Expand", size = 20.dp)
                        }
                        if (open == i) {
                            VSpace(Space.s)
                            Text(a, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                    }
                }
            }
            ListRow("Contact support", icon = R.drawable.ic_mail, onClick = {
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "Sleep & White Noise support"))
                }
            })
        }
    }
}

@Composable
fun PremiumScreen(nav: NavHostController) {
    val c = AppTheme.colors
    var plan by rememberSaveable { mutableIntStateOf(1) }
    var notice by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = "Premium", subtitle = "More soundscapes. More flexibility.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("View purchase options", { notice = true })
            TextAction("Restore purchases", { notice = true }, Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        HeroImage(painterResource(R.drawable.img_midnight), aspect = 2.2f)
        VSpace(Space.l)
        listOf("Longer sleep timers and custom fades", "Unlimited saved mixes", "Extended sleep history and reports").forEach {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(R.drawable.ic_check, null, tint = c.success, size = 20.dp)
                Spacer(Modifier.width(Space.s))
                Text(it, style = MaterialTheme.typography.bodyLarge, color = c.text)
            }
        }
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            listOf("Monthly plan", "Annual plan", "Lifetime").forEachIndexed { i, name ->
                RadioRow(name, plan == i, { plan = i }, subtitle = "Price shown by Google Play")
            }
        }
        VSpace(Space.l)
        InfoNote("Prices come from Google Play in your local currency and are confirmed before you pay.")
    }
    if (notice) {
        AlertDialog(
            onDismissRequest = { notice = false }, containerColor = c.surface,
            title = { Text("Not available yet", color = c.text) },
            text = { Text("Purchases need Google Play Billing, which isn't connected in this build. Everything in the app is free to use for now.", color = c.muted) },
            confirmButton = { TextButton(onClick = { notice = false }) { Text("OK", color = c.primary) } },
        )
    }
}
