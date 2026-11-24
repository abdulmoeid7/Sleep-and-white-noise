package com.example.sleepandwhitenoise.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.audio.PlayerState
import com.example.sleepandwhitenoise.data.Fmt
import com.example.sleepandwhitenoise.data.Sound
import com.example.sleepandwhitenoise.data.SoundCatalog
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.LocalReduceMotion
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

val CardShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(27.dp)

@Composable
fun AppIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.primary,
    size: Dp = 24.dp,
) = Icon(painterResource(id), contentDescription, modifier.size(size), tint = tint)

/**
 * Shared page template from the design references: back affordance, large title, supporting text,
 * scrollable body and a pinned primary action. Handles safe-drawing insets and the keyboard.
 */
@Composable
fun ScreenScaffold(
    title: String?,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { AppIcon(R.drawable.ic_back, "Back") }
            } else Spacer(Modifier.width(12.dp))
            Spacer(Modifier.weight(1f))
            actions()
        }
        val body = Modifier
            .weight(1f)
            .fillMaxWidth()
            .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
            .padding(horizontal = Space.gutter)
        Column(body) {
            if (title != null) {
                Text(
                    title, style = MaterialTheme.typography.headlineLarge, color = c.text,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (subtitle != null) {
                Spacer(Modifier.height(Space.s))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = c.muted)
            }
            if (title != null || subtitle != null) Spacer(Modifier.height(Space.xl))
            content()
            if (bottom == null) {
                Spacer(Modifier.height(Space.xl))
                Spacer(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)))
            } else if (scrollable) Spacer(Modifier.height(Space.l))
        }
        if (bottom != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.s),
                content = bottom,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = AppTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary, contentColor = c.onPrimary,
            disabledContainerColor = c.primary.copy(alpha = 0.35f), disabledContentColor = c.onPrimary.copy(alpha = 0.7f),
        ),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = c.onPrimary, strokeWidth = 2.5.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val c = AppTheme.colors
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp), shape = PillShape,
        border = BorderStroke(1.dp, if (danger) c.error else c.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (danger) c.error else c.primary),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AppTheme.colors.primary) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = AppTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth().clip(CardShape).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Surface(m, shape = CardShape, color = color) { Column(content = content) }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    titleColor: Color = AppTheme.colors.text,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = if (onClick != null) ({ AppIcon(R.drawable.ic_chevron_right, null, tint = AppTheme.colors.muted, size = 20.dp) }) else null,
) {
    val c = AppTheme.colors
    AppCard(modifier, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = Space.l, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(c.elevated), contentAlignment = Alignment.Center) {
                    AppIcon(icon, null, size = 20.dp)
                }
                Spacer(Modifier.width(Space.m))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            if (value != null) {
                Spacer(Modifier.width(Space.s))
                Text(value, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.End)
            }
            if (trailing != null) { Spacer(Modifier.width(Space.s)); trailing() }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    Surface(
        modifier.fillMaxWidth().clip(CardShape)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        shape = CardShape, color = c.surface,
    ) {
        Row(
            Modifier.heightIn(min = 60.dp).padding(horizontal = Space.l, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(c.elevated), contentAlignment = Alignment.Center) {
                    AppIcon(icon, null, size = 20.dp)
                }
                Spacer(Modifier.width(Space.m))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            Switch(
                checked = checked, onCheckedChange = null, enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary,
                    uncheckedThumbColor = c.muted, uncheckedTrackColor = c.elevated, uncheckedBorderColor = c.border,
                ),
            )
        }
    }
}

@Composable
fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val c = AppTheme.colors
    Surface(
        modifier.fillMaxWidth().clip(CardShape).selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = CardShape, color = if (selected) c.elevated else c.surface,
        border = if (selected) BorderStroke(1.5.dp, c.primary) else null,
    ) {
        Row(Modifier.heightIn(min = 60.dp).padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = c.text)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            if (selected) AppIcon(R.drawable.ic_check, "Selected", size = 22.dp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.text,
            modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null && onAction != null) TextAction(action, onAction)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    password: Boolean = false,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onIme: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val c = AppTheme.colors
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { AppIcon(it, null, tint = c.muted, size = 20.dp) } },
        trailingIcon = if (password) ({
            IconButton(onClick = { visible = !visible }) {
                AppIcon(R.drawable.ic_eye, if (visible) "Hide password" else "Show password", tint = c.muted, size = 20.dp)
            }
        }) else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onIme?.invoke() }),
        shape = CardShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, disabledContainerColor = c.surface,
            errorContainerColor = c.surface,
            focusedBorderColor = c.primary, unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = c.primary, unfocusedLabelColor = c.muted,
            cursorColor = c.primary, focusedTextColor = c.text, unfocusedTextColor = c.text,
            errorBorderColor = c.error, errorLabelColor = c.error, errorSupportingTextColor = c.error,
        ),
    )
}

@Composable
fun SearchBarButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val c = AppTheme.colors
    Surface(modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick, role = Role.Button),
        color = c.surface, shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.padding(start = Space.l, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(R.drawable.ic_search, null, size = 22.dp)
            Spacer(Modifier.width(Space.m))
            Text(text, color = c.muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}

@Composable
fun AppChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = c.surface, labelColor = c.muted,
            selectedContainerColor = c.primary, selectedLabelColor = c.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = c.border, selectedBorderColor = c.primary),
    )
}

@Composable
fun AppSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, label: String, steps: Int = 0, range: ClosedFloatingPointRange<Float> = 0f..1f) {
    val c = AppTheme.colors
    Slider(
        value = value, onValueChange = onValueChange, valueRange = range, steps = steps,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.elevated,
            activeTickColor = c.onPrimary.copy(alpha = 0.4f), inactiveTickColor = c.border),
    )
}

/** Sound card from the library grid: scenic artwork with a name/play footer. */
@Composable
fun SoundTile(sound: Sound, playing: Boolean, onOpen: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Surface(modifier.fillMaxWidth().clip(CardShape).clickable(onClick = onOpen), shape = CardShape, color = c.surface) {
        Column {
            Image(
                painterResource(sound.image), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.55f),
            )
            Row(Modifier.padding(start = Space.m, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(sound.name, style = MaterialTheme.typography.bodyLarge, color = c.text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = onPlay) {
                    AppIcon(if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                        if (playing) "Pause ${sound.name}" else "Play ${sound.name}", size = 22.dp)
                }
            }
        }
    }
}

/** Row used on Home and in lists: waveform glyph, name and play/pause. */
@Composable
fun SoundRow(sound: Sound, playing: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, onOpen: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = AppTheme.colors
    AppCard(modifier, onClick = onOpen ?: onToggle) {
        Row(Modifier.heightIn(min = 72.dp).padding(start = Space.m, end = 4.dp, top = Space.s, bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(sound.image), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(sound.name, style = MaterialTheme.typography.bodyLarge, color = c.text)
                Text(subtitle ?: sound.category.title, style = MaterialTheme.typography.bodyMedium, color = if (playing) c.primary else c.muted)
            }
            trailing?.invoke()
            IconButton(onClick = onToggle) {
                AppIcon(if (playing) R.drawable.ic_pause else R.drawable.ic_play, if (playing) "Pause ${sound.name}" else "Play ${sound.name}")
            }
        }
    }
}

@Composable
fun HeroImage(painter: Painter, modifier: Modifier = Modifier, aspect: Float = 1.6f, overlay: @Composable (ColumnScope.() -> Unit)? = null) {
    Box(modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(24.dp))) {
        Image(painter, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (overlay != null) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x990B1020), Color.Transparent, Color(0x660B1020)))))
            Column(Modifier.fillMaxSize().padding(Space.gutter), content = overlay)
        }
    }
}

@Composable
fun IconBadge(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 128.dp, color: Color = AppTheme.colors.primary) {
    Box(modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 0.66f).clip(CircleShape).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            AppIcon(icon, null, tint = color, size = size * 0.32f)
        }
    }
}

@Composable
fun Dots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(modifier.semantics { contentDescription = "Page ${selected + 1} of $count" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            Box(Modifier.height(8.dp).width(if (i == selected) 24.dp else 8.dp).clip(CircleShape)
                .background(if (i == selected) c.primary else c.border))
        }
    }
}

/** Mirrored bar waveform. With no live [levels] it shows a gentle idle animation (unless reduce motion). */
@Composable
fun Waveform(levels: List<Float>, modifier: Modifier = Modifier, bars: Int = 40, active: Boolean = true) {
    val c = AppTheme.colors
    val reduce = LocalReduceMotion.current
    val phase = if (!reduce && active && levels.isEmpty()) {
        rememberInfiniteTransition(label = "wave").animateFloat(0f, (2 * PI).toFloat(),
            infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else 0f
    Canvas(modifier.fillMaxWidth().height(72.dp)) {
        val gap = size.width / bars
        val w = gap * 0.45f
        for (i in 0 until bars) {
            val v = if (levels.isNotEmpty()) {
                val idx = (i * levels.size / bars).coerceIn(0, levels.size - 1)
                ((levels[idx] - 20f) / 60f).coerceIn(0.06f, 1f)
            } else if (!active) 0.08f
            else (0.25f + 0.55f * abs(sin(i * 0.45f + phase)) * (0.6f + 0.4f * sin(i * 0.13f + phase / 2))).coerceIn(0.08f, 1f)
            val h = size.height * v
            drawRoundRect(
                c.primary.copy(alpha = if (active) 1f else 0.4f),
                topLeft = Offset(i * gap + (gap - w) / 2, (size.height - h) / 2),
                size = Size(w, h), cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}

/** Simple bar chart; values are 0..1, null renders as an empty slot. */
@Composable
fun BarChart(values: List<Float?>, labels: List<String>, modifier: Modifier = Modifier, highlightLast: Boolean = true, height: Dp = 140.dp) {
    val c = AppTheme.colors
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val w = slot * 0.56f
            values.forEachIndexed { i, v ->
                val x = i * slot + (slot - w) / 2
                if (v == null) {
                    drawRoundRect(c.border, Offset(x, size.height - 6f), Size(w, 6f), CornerRadius(3f))
                } else {
                    val h = (size.height * v.coerceIn(0.04f, 1f))
                    val hl = highlightLast && i == values.lastIndex
                    drawRoundRect(if (hl) c.primary else c.primary.copy(alpha = 0.45f), Offset(x, size.height - h), Size(w, h), CornerRadius(14f))
                }
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(Space.s))
            Row(Modifier.fillMaxWidth()) {
                labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center, maxLines = 1) }
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppTheme.colors.text) {
    val c = AppTheme.colors
    Surface(modifier, shape = CardShape, color = c.surface) {
        Column(Modifier.padding(Space.l)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            Spacer(Modifier.height(Space.xs))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        }
    }
}

/** Featured metric panel used across tracking and report screens. */
@Composable
fun MetricPanel(eyebrow: String, value: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    val c = AppTheme.colors
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = c.surface) {
        Column(Modifier.padding(Space.gutter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = c.muted)
            Spacer(Modifier.height(Space.s))
            Text(value, style = MaterialTheme.typography.displayLarge, color = c.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Space.l))
            content()
        }
    }
}

/** Centered illustration + message: the empty, loading, error, success and permission templates. */
@Composable
fun StateContent(
    @DrawableRes illustration: Int?,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
    success: Boolean = false,
) {
    val c = AppTheme.colors
    Column(modifier.fillMaxWidth().padding(vertical = Space.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            success -> SuccessBadge()
            illustration != null -> Image(painterResource(illustration), null, Modifier.size(180.dp))
            icon != null -> IconBadge(icon)
        }
        if (loading) {
            Spacer(Modifier.height(Space.xl))
            CircularProgressIndicator(color = c.primary, trackColor = c.elevated)
        }
        Spacer(Modifier.height(Space.xl))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
    }
}

/**
 * Success illustration in the style of the design pack's success.png (teal disc, dark mark).
 * Drawn in code because the shipped PNG's checkmark glyph failed to render (shows a "?" box).
 */
@Composable
fun SuccessBadge(size: Dp = 180.dp) {
    val c = AppTheme.colors
    Box(Modifier.size(size).clip(CircleShape).background(c.secondary), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(size * 0.42f), tint = Color(0xFF0B1020))
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    dismiss: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.text) },
        text = { Text(text, color = c.muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (destructive) c.error else c.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss, color = c.muted) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(title: String, initialMin: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val c = AppTheme.colors
    val state = rememberTimePickerState(initialHour = initialMin / 60, initialMinute = initialMin % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.text) },
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Set", color = c.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.muted) } },
    )
}

/** Persistent mini-player (design: "Mini-player state"). */
@Composable
fun MiniPlayer(state: PlayerState, onOpen: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val sound = state.primarySoundId?.let { SoundCatalog.get(it) } ?: return
    Surface(
        modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = 6.dp).clip(CardShape).clickable(onClick = onOpen)
            .border(1.dp, c.border, CardShape),
        color = c.elevated, shape = CardShape,
    ) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(sound.image), null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = state.timerRemainingMs?.let { "Timer ${Fmt.minutesLeft(it)}" } ?: if (state.playing) "Playing" else "Paused"
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
            IconButton(onClick = onToggle) {
                AppIcon(if (state.playing) R.drawable.ic_pause else R.drawable.ic_play, if (state.playing) "Pause" else "Play")
            }
            IconButton(onClick = onClose) { AppIcon(R.drawable.ic_close, "Stop and close player", tint = c.muted, size = 20.dp) }
        }
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.muted) {
    val c = AppTheme.colors
    Row(modifier.fillMaxWidth().clip(CardShape).background(c.elevated.copy(alpha = 0.6f)).padding(Space.m), verticalAlignment = Alignment.Top) {
        AppIcon(R.drawable.ic_shield, null, tint = c.secondary, size = 20.dp)
        Spacer(Modifier.width(Space.s))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
