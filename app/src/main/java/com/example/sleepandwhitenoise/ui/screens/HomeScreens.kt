package com.example.sleepandwhitenoise.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.audio.PlayerState
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.MixLayer
import com.example.sleepandwhitenoise.data.SoundCatalog
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppCard
import com.example.sleepandwhitenoise.ui.components.AppChip
import com.example.sleepandwhitenoise.ui.components.AppIcon
import com.example.sleepandwhitenoise.ui.components.AppTextField
import com.example.sleepandwhitenoise.ui.components.HeroImage
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.SectionTitle
import com.example.sleepandwhitenoise.ui.components.SoundRow
import com.example.sleepandwhitenoise.ui.components.StateContent
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space

/** True when [id] is the only sound loaded and it is playing. */
fun PlayerState.isPlayingSound(id: String) = playing && layers.size == 1 && layers[0].soundId == id

fun SleepApp.toggleSound(id: String) {
    val st = player.state.value
    if (st.layers.size == 1 && st.layers[0].soundId == id) player.toggle() else player.playSound(id)
}

@Composable
fun HomeScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val tracking by app.tracking.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = Fmt.greeting(),
        subtitle = if (s.displayName.isNotBlank()) "${s.displayName}, make time for rest." else "Make time for rest.",
        actions = {
            IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { AppIcon(R.drawable.ic_search, "Search sounds") }
            IconButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) { AppIcon(R.drawable.ic_bell, "Notifications") }
        },
        bottom = {
            PrimaryButton(if (tracking.active) "Return to sleep session" else "Start sleep tracking", {
                nav.navigate(if (tracking.active) Routes.TRACKING else Routes.SESSION_SETUP)
            })
        },
    ) {
        HeroImage(painterResource(R.drawable.img_midnight), aspect = 1.7f) {
            Text("Your quiet moment", style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color.White)
            Text("Tonight is yours.", style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color(0xFFE6E8F5))
            Spacer(Modifier.weight(1f))
            Text(
                "Bedtime ${Fmt.timeOfDay(s.bedtimeMin)}" + if (s.alarmEnabled) "  •  Alarm ${Fmt.timeOfDay(s.alarmMin)}" else "",
                style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFFE6E8F5),
            )
        }
        VSpace(Space.xl)
        SectionTitle("Quick sounds", action = "For tonight", onAction = { nav.navigate(Routes.RECOMMENDATIONS) })
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            listOf("rain", "ocean", "brown").forEach { id ->
                val snd = SoundCatalog.require(id)
                SoundRow(snd, player.isPlayingSound(id), onToggle = { app.toggleSound(id) }, onOpen = { nav.navigate(Routes.sound(id)) })
            }
        }
        VSpace(Space.xl)
        SectionTitle("Your space")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Shortcut("Mixer", R.drawable.ic_mix, Modifier.weight(1f)) { nav.navigate(Routes.MIXER) }
            Shortcut("Favorites", R.drawable.ic_heart, Modifier.weight(1f)) { nav.navigate(Routes.FAVORITES) }
            Shortcut("My mixes", R.drawable.ic_sounds, Modifier.weight(1f)) { nav.navigate(Routes.MIXES) }
        }
    }
}

@Composable
private fun Shortcut(label: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    AppCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(Space.m), verticalArrangement = Arrangement.SpaceBetween) {
            AppIcon(icon, null)
            VSpace(Space.s)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
    }
}

@Composable
fun NotificationsScreen(app: SleepApp, nav: NavHostController) {
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val sessions by app.sessions.sessions.collectAsStateWithLifecycle()
    val latest = sessions.firstOrNull()
    ScreenScaffold(
        title = "Notifications", subtitle = "Your bedtime updates.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Manage reminders", { nav.navigate(Routes.REMINDER_SETTINGS) }) },
    ) {
        val items = buildList {
            if (s.reminderEnabled) add(Triple("Bedtime reminder", Fmt.timeOfDay(s.reminderMin), R.drawable.ic_bell) to { nav.navigate(Routes.REMINDER_SETTINGS) })
            if (s.alarmEnabled) add(Triple("Wake-up alarm", Fmt.timeOfDay(s.alarmMin), R.drawable.ic_alarm) to { nav.navigate(Routes.ALARM) })
            if (latest != null) add(Triple("Sleep report ready", Fmt.dayShort(latest.endMs), R.drawable.ic_insights) to { nav.navigate(Routes.report(latest.id)) })
        }
        if (items.isEmpty()) {
            StateContent(R.drawable.ill_empty, "All quiet", "Turn on a bedtime reminder or record a session to see updates here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                items.forEach { (t, action) -> ListRow(t.first, value = t.second, icon = t.third, onClick = action) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = SoundCatalog.search(query)
    ScreenScaffold(
        title = if (query.isBlank()) "Search" else "Results for ${query.trim()}",
        subtitle = if (query.isBlank()) "Find your favorite sounds." else "${results.size} sound${if (results.size == 1) "" else "s"} • all available offline",
        onBack = { nav.popBackStack() },
        bottom = if (results.isNotEmpty()) {
            {
                val first = results.first()
                PrimaryButton("Play ${first.name.lowercase()}", {
                    app.settings.addRecentSearch(query)
                    app.player.playSound(first.id)
                    nav.navigate(Routes.PLAYER)
                })
            }
        } else null,
    ) {
        AppTextField(
            query, { query = it }, "Search sounds", Modifier.focusRequester(focus), leadingIcon = R.drawable.ic_search,
            imeAction = ImeAction.Search, onIme = { app.settings.addRecentSearch(query); keyboard?.hide() },
        )
        VSpace(Space.l)
        if (query.isBlank()) {
            if (s.recentSearches.isNotEmpty()) {
                SectionTitle("Recent", action = "Clear", onAction = { app.settings.update { it.copy(recentSearches = emptyList()) } })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    s.recentSearches.forEach { AppChip(it, false, { query = it }) }
                }
                VSpace(Space.l)
            }
            SectionTitle("Popular")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                SoundCatalog.popularSearches.forEach { AppChip(it, false, { query = it }) }
            }
        } else if (results.isEmpty()) {
            StateContent(R.drawable.ill_empty, "No sounds found", "Try “rain”, “noise” or “nature”.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                results.forEach { snd ->
                    SoundRow(snd, player.isPlayingSound(snd.id), onToggle = { app.settings.addRecentSearch(query); app.toggleSound(snd.id) },
                        onOpen = { app.settings.addRecentSearch(query); nav.navigate(Routes.sound(snd.id)) })
                }
            }
        }
    }
}

@Composable
fun RecommendationsScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    // Favorites first, then the most popular sounds.
    val picks = (s.favorites.mapNotNull { SoundCatalog.get(it) } + SoundCatalog.all.sortedBy { it.popularity })
        .distinctBy { it.id }.take(3)
    ScreenScaffold(
        title = "For tonight", subtitle = "A quieter evening starts here.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Explore library", { nav.navigate(Routes.LIBRARY) { launchSingleTop = true } }) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            picks.forEach { snd ->
                SoundRow(snd, player.isPlayingSound(snd.id), onToggle = { app.toggleSound(snd.id) },
                    subtitle = if (snd.id in s.favorites) "From your favorites" else snd.description,
                    onOpen = { nav.navigate(Routes.sound(snd.id)) })
            }
        }
        VSpace(Space.xl)
        SectionTitle("Suggested mix")
        AppCard(onClick = {
            app.player.load(listOf(MixLayer("rain", 0.8f), MixLayer("brown", 0.45f)), "Rain over brown noise", "Suggested mix • 2 sounds")
            app.player.play()
            nav.navigate(Routes.PLAYER)
        }) {
            Row(Modifier.padding(Space.l), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(R.drawable.ic_mix, null)
                Spacer(Modifier.width(Space.m))
                Column(Modifier.weight(1f)) {
                    Text("Rain over brown noise", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("Soft rainfall layered over a deep, steady rumble.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                }
                AppIcon(R.drawable.ic_play, "Play mix")
            }
        }
    }
}
