package com.example.sleepandwhitenoise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.audio.PlayerController
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.MixLayer
import com.example.sleepandwhitenoise.data.SoundCatalog
import com.example.sleepandwhitenoise.data.SoundCategory
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppCard
import com.example.sleepandwhitenoise.ui.components.AppChip
import com.example.sleepandwhitenoise.ui.components.AppIcon
import com.example.sleepandwhitenoise.ui.components.AppSlider
import com.example.sleepandwhitenoise.ui.components.AppTextField
import com.example.sleepandwhitenoise.ui.components.CardShape
import com.example.sleepandwhitenoise.ui.components.ConfirmDialog
import com.example.sleepandwhitenoise.ui.components.HeroImage
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.ListRow
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.RadioRow
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.SearchBarButton
import com.example.sleepandwhitenoise.ui.components.SecondaryButton
import com.example.sleepandwhitenoise.ui.components.SectionTitle
import com.example.sleepandwhitenoise.ui.components.SoundRow
import com.example.sleepandwhitenoise.ui.components.SoundTile
import com.example.sleepandwhitenoise.ui.components.StateContent
import com.example.sleepandwhitenoise.ui.components.SwitchRow
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space

private enum class SortBy(val label: String) { POPULAR("Most popular"), NAME("Name A–Z") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val player by app.player.state.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<SoundCategory?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortBy.POPULAR) }
    var showFilter by remember { mutableStateOf(false) }
    val sounds = SoundCatalog.all
        .filter { category == null || it.category == category }
        .let { if (sort == SortBy.POPULAR) it.sortedBy { s -> s.popularity } else it.sortedBy { s -> s.name } }

    ScreenScaffold(
        title = "Sound library", subtitle = "Discover your perfect atmosphere.", scrollable = false,
        actions = { IconButton(onClick = { nav.navigate(Routes.FAVORITES) }) { AppIcon(R.drawable.ic_heart, "Favorites") } },
        bottom = { PrimaryButton("Open mixer", { nav.navigate(Routes.MIXER) }) },
    ) {
        SearchBarButton("Search sounds", { nav.navigate(Routes.SEARCH) }) {
            IconButton(onClick = { showFilter = true }) { AppIcon(R.drawable.ic_filter, "Filter and sort") }
        }
        VSpace(Space.s)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            AppChip("All", category == null, { category = null })
            SoundCategory.entries.forEach { cat -> AppChip(cat.title, category == cat, { category = cat }) }
        }
        LazyVerticalGrid(
            GridCells.Adaptive(150.dp), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.l),
            contentPadding = PaddingValues(top = Space.s, bottom = Space.l),
        ) {
            category?.let { cat ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ListRow("${cat.title} collection", subtitle = cat.blurb, onClick = { nav.navigate(Routes.category(cat.name)) })
                }
            }
            items(sounds, key = { it.id }) { snd ->
                SoundTile(snd, player.isPlayingSound(snd.id), onOpen = { nav.navigate(Routes.sound(snd.id)) }, onPlay = { app.toggleSound(snd.id) })
            }
        }
    }

    if (showFilter) {
        var draftCat by remember { mutableStateOf(category) }
        var draftSort by remember { mutableStateOf(sort) }
        ModalBottomSheet(onDismissRequest = { showFilter = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.background) {
            Column(Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.xl)) {
                Text("Filter and sorting", style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text("Customize your sound library.", style = MaterialTheme.typography.bodyLarge, color = c.muted)
                VSpace(Space.l)
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    RadioRow("All categories", draftCat == null, { draftCat = null })
                    SoundCategory.entries.forEach { cat -> RadioRow(cat.title, draftCat == cat, { draftCat = cat }, subtitle = cat.blurb) }
                }
                VSpace(Space.l)
                Text("Sort", style = MaterialTheme.typography.titleMedium, color = c.text)
                VSpace(Space.s)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    SortBy.entries.forEach { AppChip(it.label, draftSort == it, { draftSort = it }) }
                }
                VSpace(Space.m)
                SwitchRow("Offline only", true, {}, subtitle = "Every sound is generated on this device", enabled = false)
                VSpace(Space.l)
                PrimaryButton("Apply filters", { category = draftCat; sort = draftSort; showFilter = false })
            }
        }
    }
}

@Composable
fun CategoryScreen(app: SleepApp, nav: NavHostController, catName: String) {
    val cat = runCatching { SoundCategory.valueOf(catName) }.getOrDefault(SoundCategory.NATURE)
    val player by app.player.state.collectAsStateWithLifecycle()
    val sounds = SoundCatalog.all.filter { it.category == cat }.sortedBy { it.popularity }
    ScreenScaffold(
        title = "${cat.title} sounds", subtitle = "${cat.blurb} • Available offline", onBack = { nav.popBackStack() },
        bottom = {
            val first = sounds.first()
            PrimaryButton("Play ${first.name.lowercase()}", { app.player.playSound(first.id); nav.navigate(Routes.PLAYER) })
        },
    ) {
        HeroImage(painterResource(sounds.first().image), aspect = 2f)
        VSpace(Space.l)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            sounds.forEach { snd ->
                SoundRow(snd, player.isPlayingSound(snd.id), onToggle = { app.toggleSound(snd.id) },
                    subtitle = snd.description, onOpen = { nav.navigate(Routes.sound(snd.id)) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundDetailsScreen(app: SleepApp, nav: NavHostController, id: String) {
    val c = AppTheme.colors
    val snd = SoundCatalog.require(id)
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val fav = snd.id in s.favorites
    ScreenScaffold(
        title = snd.name, subtitle = snd.description, onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { app.settings.toggleFavorite(snd.id) }) {
                AppIcon(R.drawable.ic_heart, if (fav) "Remove from favorites" else "Add to favorites", tint = if (fav) c.error else c.primary)
            }
        },
        bottom = {
            PrimaryButton(if (player.isPlayingSound(snd.id)) "Open player" else "Play sound", {
                if (!player.isPlayingSound(snd.id)) app.player.playSound(snd.id)
                nav.navigate(Routes.PLAYER)
            })
            SecondaryButton("Add to mixer", {
                if (!player.hasContent) app.player.load(listOf(MixLayer(snd.id, 0.8f)), "Custom mix", "Custom mix • 1 sound")
                else app.player.addLayer(snd.id)
                nav.navigate(Routes.MIXER)
            })
        },
    ) {
        HeroImage(painterResource(snd.image), aspect = 1.25f)
        VSpace(Space.l)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(snd.category.title, "Available offline", "Seamless loop", "Generated on device").forEach { Tag(it) }
        }
        VSpace(Space.l)
        Text(
            if (fav) "In your favorites." else "Tap the heart to keep this sound in your favorites.",
            style = MaterialTheme.typography.bodyMedium, color = c.muted,
        )
    }
}

@Composable
private fun Tag(text: String) {
    val c = AppTheme.colors
    Text(text, style = MaterialTheme.typography.bodyMedium, color = c.secondary,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
fun PlayerScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val st by app.player.state.collectAsStateWithLifecycle()
    val s by app.settings.settings.collectAsStateWithLifecycle()
    if (!st.hasContent) {
        ScreenScaffold(title = "Player", onBack = { nav.popBackStack() },
            bottom = { PrimaryButton("Browse sounds", { nav.navigate(Routes.LIBRARY) { launchSingleTop = true } }) }) {
            StateContent(R.drawable.ill_empty, "Nothing playing", "Pick a sound or a mix to start listening.")
        }
        return
    }
    val primary = SoundCatalog.require(st.primarySoundId!!)
    val fav = primary.id in s.favorites
    ScreenScaffold(
        title = null, onBack = { nav.popBackStack() },
        actions = { TextAction("Edit mix", { nav.navigate(Routes.MIXER) }) },
        bottom = {
            if (st.timerRemainingMs != null) SecondaryButton("Cancel timer", { app.player.cancelTimer() })
            else PrimaryButton("Set sleep timer", { nav.navigate(Routes.TIMER) })
        },
    ) {
        Image(
            painterResource(primary.image), null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(28.dp)),
        )
        VSpace(Space.xl)
        Text(st.title, style = MaterialTheme.typography.headlineLarge, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(st.subtitle, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        VSpace(Space.l)
        val remaining = st.timerRemainingMs
        if (remaining != null && st.timerTotalMs > 0) {
            LinearProgressIndicator(
                progress = { 1f - remaining / st.timerTotalMs.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = c.primary, trackColor = c.elevated,
                drawStopIndicator = {},
            )
            VSpace(Space.s)
            Text(
                "${Fmt.minutesLeft(remaining)} left" + if (st.fadeMs > 0) " • fades out over ${st.fadeMs / 60_000} min" else "",
                style = MaterialTheme.typography.bodyMedium, color = c.muted,
            )
        } else {
            Text("Looping • plays until you stop it", style = MaterialTheme.typography.bodyMedium, color = c.muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        VSpace(Space.l)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { app.settings.toggleFavorite(primary.id) }, Modifier.size(56.dp)) {
                AppIcon(R.drawable.ic_heart, if (fav) "Remove ${primary.name} from favorites" else "Add ${primary.name} to favorites",
                    tint = if (fav) c.error else c.primary, size = 28.dp)
            }
            FilledIconButton(
                onClick = { app.player.toggle() }, Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                AppIcon(if (st.playing) R.drawable.ic_pause else R.drawable.ic_play, if (st.playing) "Pause" else "Play", tint = c.onPrimary, size = 36.dp)
            }
            IconButton(onClick = { nav.navigate(Routes.TIMER) }, Modifier.size(56.dp)) {
                AppIcon(R.drawable.ic_timer, "Sleep timer", size = 28.dp)
            }
        }
        VSpace(Space.l)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(R.drawable.ic_volume, null, tint = c.muted, size = 20.dp)
            Spacer(Modifier.width(Space.s))
            AppSlider(st.volume, { app.player.setVolume(it) }, Modifier.weight(1f), label = "Volume")
        }
    }
}

@Composable
fun SleepTimerScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val st by app.player.state.collectAsStateWithLifecycle()
    val s by app.settings.settings.collectAsStateWithLifecycle()
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customMin by rememberSaveable { mutableFloatStateOf(45f) }
    var fade by rememberSaveable { mutableIntStateOf(s.fadeOutMin) }
    val chosen = if (custom) customMin.toInt() else minutes
    ScreenScaffold(
        title = "Sleep timer", subtitle = "Let your audio fade out gently.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Start timer", {
                app.player.startTimer(chosen, fade.coerceAtMost(chosen))
                nav.popBackStack()
            }, enabled = st.hasContent)
            if (!st.hasContent) Text("Play a sound first, then set a timer.", color = c.muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(15, 30, 60, 90).forEach { m ->
                RadioRow("$m minutes", !custom && minutes == m, { custom = false; minutes = m })
            }
            RadioRow("Custom duration", custom, { custom = true }, subtitle = if (custom) Fmt.duration(customMin.toLong() * 60_000) else null)
            if (custom) AppSlider(customMin, { customMin = (it / 5).toInt() * 5f }, label = "Custom duration in minutes", range = 5f..240f)
        }
        VSpace(Space.xl)
        Text("Fade out", style = MaterialTheme.typography.titleMedium, color = c.text)
        VSpace(Space.s)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(0, 1, 5, 10).forEach { f -> AppChip(if (f == 0) "Off" else "$f min", fade == f, { fade = f }) }
        }
    }
}

@Composable
fun MixerScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val st by app.player.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (!app.player.state.value.hasContent) {
            app.player.load(listOf(MixLayer("rain", 0.7f), MixLayer("ocean", 0.5f), MixLayer("brown", 0.35f)), "Custom mix", "Custom mix • 3 sounds")
        }
    }
    ScreenScaffold(
        title = "Sound mixer", subtitle = "Layer sounds to create your own atmosphere.", onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { app.player.toggle() }, enabled = st.hasContent) {
                AppIcon(if (st.playing) R.drawable.ic_pause else R.drawable.ic_play, if (st.playing) "Pause mix" else "Play mix")
            }
        },
        bottom = { PrimaryButton("Save mix", { nav.navigate(Routes.SAVE_MIX) }, enabled = st.hasContent) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            st.layers.forEach { layer ->
                val snd = SoundCatalog.require(layer.soundId)
                AppCard {
                    Column(Modifier.padding(start = Space.l, end = 4.dp, top = Space.s, bottom = Space.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(snd.image), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(Space.m))
                            Text(snd.name, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                            Text("${(layer.volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                            IconButton(onClick = { app.player.removeLayer(snd.id) }) {
                                AppIcon(R.drawable.ic_minus, "Remove ${snd.name}", tint = c.muted, size = 20.dp)
                            }
                        }
                        AppSlider(layer.volume, { app.player.setLayerVolume(snd.id, it) }, Modifier.padding(end = Space.m),
                            label = "${snd.name} volume")
                    }
                }
            }
            if (st.layers.size < PlayerController.MAX_LAYERS) {
                ListRow("Add sound", icon = R.drawable.ic_plus, titleColor = c.primary, onClick = { nav.navigate(Routes.ADD_SOUND) })
            } else {
                Text("Mixes can have up to ${PlayerController.MAX_LAYERS} sounds.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            if (!st.playing && st.hasContent) {
                SecondaryButton("Preview mix", { app.player.play() })
            }
        }
    }
}

@Composable
fun AddSoundScreen(app: SleepApp, nav: NavHostController) {
    val st by app.player.state.collectAsStateWithLifecycle()
    val available = SoundCatalog.all.filter { s -> st.layers.none { it.soundId == s.id } }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenScaffold(
        title = "Add sound", subtitle = "Choose a sound to add to your mix.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Add selected sound", {
                selected?.let { app.player.addLayer(it) }
                nav.popBackStack()
            }, enabled = selected != null)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            available.forEach { snd ->
                RadioRow(snd.name, selected == snd.id, { selected = snd.id }, subtitle = snd.category.title)
            }
        }
    }
}

@Composable
fun SaveMixScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val st by app.player.state.collectAsStateWithLifecycle()
    val s by app.settings.settings.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    val duplicate = s.mixes.any { it.name.equals(name.trim(), ignoreCase = true) }
    ScreenScaffold(
        title = "Save custom mix", subtitle = "Give your soundscape a name.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Save mix", {
                val mix = app.settings.saveMix(name, st.layers)
                app.player.load(mix.layers, mix.name, "Custom mix • ${mix.layers.size} sounds")
                nav.navigate(Routes.MIX_SAVED) { popUpTo(Routes.MIXER) { inclusive = true } }
            }, enabled = name.isNotBlank() && !duplicate && st.hasContent)
        },
    ) {
        AppTextField(name, { name = it.take(40) }, "Mix name", leadingIcon = R.drawable.ic_edit,
            error = if (duplicate) "You already have a mix with this name" else null)
        VSpace(Space.l)
        Text("In this mix", style = MaterialTheme.typography.titleMedium, color = c.text)
        VSpace(Space.s)
        st.layers.forEach { l ->
            Text("${SoundCatalog.require(l.soundId).name} • ${(l.volume * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge,
                color = c.muted, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun MixSavedScreen(nav: NavHostController) {
    ScreenScaffold(
        title = null, onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("View my mixes", { nav.navigate(Routes.MIXES) { popUpTo(Routes.MIX_SAVED) { inclusive = true } } }) },
    ) {
        StateContent(null, "Mix saved", "Your sound mix has been saved.", success = true)
    }
}

@Composable
fun MyMixesScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val st by app.player.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<String?>(null) }
    ScreenScaffold(
        title = "My mixes", subtitle = "Your personal soundscapes.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Create mix", { nav.navigate(Routes.MIXER) }) },
    ) {
        if (s.mixes.isEmpty()) {
            StateContent(R.drawable.ill_empty, "No mixes yet", "Layer a few sounds in the mixer and save them here.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            s.mixes.forEach { mix ->
                val playing = st.playing && st.title == mix.name && st.layers == mix.layers
                val img = SoundCatalog.require(mix.layers.firstOrNull()?.soundId ?: "rain").image
                AppCard(onClick = { if (playing) app.player.pause() else app.player.playMix(mix) }) {
                    Row(Modifier.padding(start = Space.m, end = 4.dp, top = Space.s, bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(img), null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                        Spacer(Modifier.width(Space.m))
                        Column(Modifier.weight(1f)) {
                            Text(mix.name, style = MaterialTheme.typography.bodyLarge, color = c.text)
                            Text(mix.layers.joinToString(" • ") { SoundCatalog.require(it.soundId).name }, style = MaterialTheme.typography.bodyMedium,
                                color = if (playing) c.primary else c.muted, maxLines = 2)
                        }
                        IconButton(onClick = { deleting = mix.id }) { AppIcon(R.drawable.ic_trash, "Delete ${mix.name}", tint = c.muted, size = 20.dp) }
                        IconButton(onClick = { if (playing) app.player.pause() else app.player.playMix(mix) }) {
                            AppIcon(if (playing) R.drawable.ic_pause else R.drawable.ic_play, if (playing) "Pause ${mix.name}" else "Play ${mix.name}")
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        val name = s.mixes.firstOrNull { it.id == id }?.name ?: "this mix"
        ConfirmDialog("Delete mix?", "“$name” will be removed from your mixes.", "Delete", {
            app.settings.deleteMix(id); deleting = null
        }, "Cancel", { deleting = null }, destructive = true)
    }
}

@Composable
fun FavoritesScreen(app: SleepApp, nav: NavHostController) {
    val c = AppTheme.colors
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val player by app.player.state.collectAsStateWithLifecycle()
    val favs = SoundCatalog.all.filter { it.id in s.favorites }
    ScreenScaffold(
        title = "Favorites", subtitle = "Your favorite sounds, ready to play.", onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Explore sounds", { nav.navigate(Routes.LIBRARY) { launchSingleTop = true } }) },
    ) {
        if (favs.isEmpty()) {
            StateContent(R.drawable.ill_empty, "No favorites yet", "Tap the heart on any sound to keep it here.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            favs.forEach { snd ->
                SoundRow(snd, player.isPlayingSound(snd.id), onToggle = { app.toggleSound(snd.id) },
                    onOpen = { nav.navigate(Routes.sound(snd.id)) },
                    trailing = {
                        IconButton(onClick = { app.settings.toggleFavorite(snd.id) }) {
                            AppIcon(R.drawable.ic_heart, "Remove ${snd.name} from favorites", tint = c.error, size = 20.dp)
                        }
                    })
            }
        }
    }
}
