package com.example.sleepandwhitenoise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tokens from app_design/design_system/colors.md ("Midnight dark"). */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val isDark: Boolean,
)

val MidnightColors = AppColors(
    primary = Color(0xFFB5A4FF),
    onPrimary = Color(0xFF0B1020),
    secondary = Color(0xFFA9DCD7),
    accent = Color(0xFFF5E2B8),
    background = Color(0xFF0B1020),
    surface = Color(0xFF151B30),
    elevated = Color(0xFF202842),
    text = Color(0xFFF4F5FC),
    muted = Color(0xFFA4ADC5),
    border = Color(0xFF303A55),
    success = Color(0xFF69D5A3),
    error = Color(0xFFFF8E9A),
    warning = Color(0xFFF4C477),
    isDark = true,
)

/**
 * The design pack only specifies a dark palette and notes that "system theme requires
 * additional light palette design". This is a provisional light counterpart derived from
 * the same hues, used only when the user picks "System theme" and the device is light.
 */
val DawnColors = AppColors(
    primary = Color(0xFF5B48C9),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2E7F78),
    accent = Color(0xFF8A6A1F),
    background = Color(0xFFF6F5FC),
    surface = Color(0xFFFFFFFF),
    elevated = Color(0xFFECEAF8),
    text = Color(0xFF151B30),
    muted = Color(0xFF5A6380),
    border = Color(0xFFD6D9E6),
    success = Color(0xFF1E8A5A),
    error = Color(0xFFC0384A),
    warning = Color(0xFF9A6A00),
    isDark = false,
)

val LocalAppColors = staticCompositionLocalOf { MidnightColors }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Spacing scale from spacing.md. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val gutter = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
}

/** Type scale from typography.md (Roboto on Android). */
private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
)

@Composable
fun SleepTheme(
    followSystem: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (followSystem && !isSystemInDarkTheme()) DawnColors else MidnightColors
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.secondary, onSecondary = colors.onPrimary,
            tertiary = colors.accent,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.elevated, onSurfaceVariant = colors.muted,
            surfaceContainerHigh = colors.elevated, surfaceContainer = colors.surface,
            surfaceContainerHighest = colors.elevated, surfaceContainerLow = colors.surface,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.error, onError = colors.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.secondary, onSecondary = Color.White,
            tertiary = colors.accent,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.elevated, onSurfaceVariant = colors.muted,
            surfaceContainerHigh = colors.elevated, surfaceContainer = colors.surface,
            surfaceContainerHighest = colors.elevated, surfaceContainerLow = colors.surface,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.error, onError = Color.White,
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
}
