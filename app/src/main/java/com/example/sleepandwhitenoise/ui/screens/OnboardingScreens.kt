package com.example.sleepandwhitenoise.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.alarm.AlarmScheduler
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppSlider
import com.example.sleepandwhitenoise.ui.components.Dots
import com.example.sleepandwhitenoise.ui.components.IconBadge
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.TimePickerDialog
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.resetTo
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        app.settings.loaded.first { it }
        delay((1400 - (System.currentTimeMillis() - start)).coerceAtLeast(0))
        val tracking = app.tracking.state.value
        when {
            !app.settings.current.onboarded -> nav.resetTo(Routes.ONBOARDING)
            tracking.active -> { nav.resetTo(Routes.HOME); nav.navigate(Routes.TRACKING) }
            else -> nav.resetTo(Routes.HOME)
        }
    }
    Box(Modifier.fillMaxSize().background(c.background)) {
        Image(
            painterResource(R.drawable.ill_splash), null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(Space.gutter),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.app_logo), null, Modifier.size(104.dp))
            VSpace(Space.xl)
            Text("Sleep & White Noise", style = MaterialTheme.typography.displayLarge, color = c.text, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() })
            VSpace(Space.s)
            Text("Rest better. Wake refreshed.", style = MaterialTheme.typography.bodyLarge, color = c.muted)
            VSpace(Space.xxl)
            CircularProgressIndicator(Modifier.size(28.dp), color = c.primary, trackColor = c.elevated, strokeWidth = 2.5.dp)
        }
    }
}

private data class Page(val image: Int, val title: String, val body: String)

private val pages = listOf(
    Page(R.drawable.ill_onboarding_welcome, "Make tonight a little calmer.",
        "Soothing soundscapes, a gentle sleep timer and an optional overnight sound diary, all in one quiet place."),
    Page(R.drawable.ill_onboarding_sounds, "Find your perfect soundscape.",
        "Rain, waves, fire and colored noise. Mix them your way. All sounds work offline."),
    Page(R.drawable.ill_onboarding_tracking, "Understand your nights.",
        "Understand overnight sound activity with your microphone. Results are estimates, never a diagnosis."),
    Page(R.drawable.ill_onboarding_privacy, "Private by default.",
        "Your sleep data stays on this device by default. Audio is analyzed in memory and clips are only kept if you opt in."),
)

@Composable
fun OnboardingScreen(nav: NavHostController) {
    val c = AppTheme.colors
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    ScreenScaffold(
        title = null,
        onBack = if (pager.currentPage > 0) ({ scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) else null,
        actions = { if (!last) TextAction("Skip", { nav.navigate(Routes.PERM_MIC) }, color = c.muted) },
        scrollable = false,
        bottom = {
            Dots(pages.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally))
            VSpace(Space.l)
            PrimaryButton(if (last) "Continue" else "Next", {
                if (last) nav.navigate(Routes.PERM_MIC) else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            })
        },
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
            val p = pages[i]
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Image(
                    painterResource(p.image), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(28.dp)),
                )
                VSpace(Space.xxl)
                Text(p.title, style = MaterialTheme.typography.headlineLarge, color = c.text, modifier = Modifier.semantics { heading() })
                VSpace(Space.m)
                Text(p.body, style = MaterialTheme.typography.bodyLarge, color = c.muted)
            }
        }
    }
}

@Composable
fun MicPermissionScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val granted = remember {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        nav.navigate(Routes.PERM_NOTIF)
    }
    PermissionTemplate(
        nav, icon = R.drawable.ic_mic, title = "Microphone access",
        subtitle = "Allow microphone access only when you start a sleep session.",
        points = listOf(
            "Used only during sessions you start, with a visible notification.",
            "Sound is analyzed on this device. Nothing is uploaded.",
            "You can still play sounds without it.",
        ),
        primary = if (granted) "Continue" else "Allow microphone",
        onPrimary = { if (granted) nav.navigate(Routes.PERM_NOTIF) else launcher.launch(Manifest.permission.RECORD_AUDIO) },
        onSkip = { nav.navigate(Routes.PERM_NOTIF) },
    )
}

@Composable
fun NotificationPermissionScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val needs = Build.VERSION.SDK_INT >= 33 && remember {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        nav.navigate(Routes.GOAL_SETUP)
    }
    PermissionTemplate(
        nav, icon = R.drawable.ic_bell, title = "Notifications",
        subtitle = "Get bedtime reminders and alarm notifications.",
        points = listOf(
            "A gentle reminder before your bedtime goal.",
            "Your wake-up alarm and playback controls.",
            "No marketing messages.",
        ),
        primary = "Continue",
        onPrimary = { if (needs) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else nav.navigate(Routes.GOAL_SETUP) },
        onSkip = { nav.navigate(Routes.GOAL_SETUP) },
    )
}

@Composable
private fun PermissionTemplate(
    nav: NavHostController, icon: Int, title: String, subtitle: String, points: List<String>,
    primary: String, onPrimary: () -> Unit, onSkip: () -> Unit,
) {
    val c = AppTheme.colors
    ScreenScaffold(
        title = title, subtitle = subtitle, onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton(primary, onPrimary)
            TextAction("Not now", onSkip, Modifier.align(Alignment.CenterHorizontally), color = c.muted)
        },
    ) {
        IconBadge(icon, Modifier.align(Alignment.CenterHorizontally).padding(vertical = Space.l), size = 160.dp)
        VSpace(Space.l)
        points.forEach {
            Row(Modifier.padding(vertical = 6.dp)) {
                Text("•", color = c.primary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.size(Space.m))
                Text(it, color = c.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun GoalSetupScreen(app: SleepApp, nav: NavHostController) {
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    ScreenScaffold(
        title = "Personal sleep goal",
        subtitle = "Choose a bedtime routine that works for you.",
        onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Finish setup", {
                app.settings.update { it.copy(onboarded = true, alarmMin = it.wakeMin) }
                AlarmScheduler.rescheduleAll(ctx, app.settings.current)
                nav.resetTo(Routes.HOME)
            })
        },
    ) {
        GoalEditor(app)
        VSpace(Space.l)
        InfoNote("You can change these any time in Insights → Sleep goals. Your wake time also sets your wake-up alarm.")
    }
}

/** Shared by onboarding goal setup and Sleep goals. */
@Composable
fun GoalEditor(app: SleepApp) {
    val s by app.settings.settings.collectAsStateWithLifecycle()
    var picking by remember { mutableIntStateOf(0) } // 1 = bedtime, 2 = wake
    var target by remember(s.sleepTargetMin) { mutableStateOf(s.sleepTargetMin.toFloat()) }
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
        com.example.sleepandwhitenoise.ui.components.AppCard {
            Column(Modifier.padding(Space.l)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep target", style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                    Text(Fmt.hours(target.toInt()), style = MaterialTheme.typography.titleMedium, color = c.primary)
                }
                AppSlider(
                    target, { target = (it / 30).toInt() * 30f }, label = "Sleep target",
                    range = 300f..660f, steps = 11,
                )
                LaunchedEffect(target) {
                    delay(250)
                    if (target.toInt() != app.settings.current.sleepTargetMin) {
                        app.settings.update { it.copy(sleepTargetMin = target.toInt()) }
                    }
                }
            }
        }
        ListRow("Bedtime", value = Fmt.timeOfDay(s.bedtimeMin), icon = R.drawable.ic_sleep, onClick = { picking = 1 })
        ListRow("Wake time", value = Fmt.timeOfDay(s.wakeMin), icon = R.drawable.ic_alarm, onClick = { picking = 2 })
        val inBed = ((s.wakeMin - s.bedtimeMin + 1440) % 1440)
        val diff = inBed - s.sleepTargetMin
        Text(
            "That's ${Fmt.duration(inBed * 60_000L)} in bed" + when {
                diff < -15 -> ", ${Fmt.duration(-diff * 60_000L)} short of your target."
                else -> ", which fits your target."
            },
            style = MaterialTheme.typography.bodyMedium, color = if (diff < -15) c.warning else c.muted,
        )
    }
    if (picking != 0) {
        TimePickerDialog(
            title = if (picking == 1) "Bedtime" else "Wake time",
            initialMin = if (picking == 1) s.bedtimeMin else s.wakeMin,
            onDismiss = { picking = 0 },
            onConfirm = { m ->
                app.settings.update { if (picking == 1) it.copy(bedtimeMin = m) else it.copy(wakeMin = m) }
                picking = 0
            },
        )
    }
}
