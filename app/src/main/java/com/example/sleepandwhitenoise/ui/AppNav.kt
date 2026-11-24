package com.example.sleepandwhitenoise.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.data.ThemeMode
import com.example.sleepandwhitenoise.ui.components.MiniPlayer
import com.example.sleepandwhitenoise.ui.screens.*
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.SleepTheme
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERM_MIC = "permission/mic"
    const val PERM_NOTIF = "permission/notifications"
    const val GOAL_SETUP = "goal_setup"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT = "forgot"
    const val VERIFY = "verify"
    const val NEW_PASSWORD = "new_password"
    const val RESET_DONE = "reset_done"
    const val HOME = "home"
    const val NOTIFICATIONS = "notifications"
    const val SEARCH = "search"
    const val RECOMMENDATIONS = "recommendations"
    const val LIBRARY = "library"
    const val CATEGORY = "category/{cat}"
    const val SOUND = "sound/{id}"
    const val PLAYER = "player"
    const val TIMER = "timer"
    const val MIXER = "mixer"
    const val ADD_SOUND = "mixer/add"
    const val SAVE_MIX = "mixer/save"
    const val MIX_SAVED = "mixer/saved"
    const val MIXES = "mixes"
    const val FAVORITES = "favorites"
    const val SLEEP = "sleep"
    const val SESSION_SETUP = "session_setup"
    const val ALARM = "alarm"
    const val MIC_TEST = "mic_test"
    const val PLACEMENT = "placement"
    const val TRACKING = "tracking"
    const val ANALYZING = "analyzing"
    const val PERM_DENIED = "permission_denied"
    const val TRACKING_ERROR = "tracking_error"
    const val SUMMARY = "summary/{id}"
    const val INSIGHTS = "insights"
    const val REPORT = "report/{id}"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val HISTORY = "history"
    const val TIMELINE = "timeline/{id}"
    const val EVENT = "event/{id}/{index}"
    const val GOALS = "goals"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val AUDIO_SETTINGS = "settings/audio"
    const val TRACKING_SETTINGS = "settings/tracking"
    const val REMINDER_SETTINGS = "settings/reminders"
    const val APPEARANCE = "settings/appearance"
    const val PRIVACY = "settings/privacy"
    const val FAQ = "settings/faq"
    const val PREMIUM = "premium"

    fun category(cat: String) = "category/$cat"
    fun sound(id: String) = "sound/$id"
    fun summary(id: String) = "summary/$id"
    fun report(id: String) = "report/$id"
    fun timeline(id: String) = "timeline/$id"
    fun event(id: String, index: Int) = "event/$id/$index"

    val tabs = listOf(HOME, LIBRARY, SLEEP, INSIGHTS, PROFILE)
}

private data class Tab(val route: String, val label: String, val icon: Int)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", R.drawable.ic_home),
    Tab(Routes.LIBRARY, "Sounds", R.drawable.ic_sounds),
    Tab(Routes.SLEEP, "Sleep", R.drawable.ic_sleep),
    Tab(Routes.INSIGHTS, "Insights", R.drawable.ic_insights),
    Tab(Routes.PROFILE, "Profile", R.drawable.ic_profile),
)

/** App-wide snackbar for short confirmations ("Preferences saved"). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Leave a flow (onboarding, auth) and land on a tab with a clean back stack. */
fun NavHostController.resetTo(route: String) = navigate(route) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

@Composable
fun AppRoot(app: SleepApp, openRequests: MutableStateFlow<String?>) {
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    SleepTheme(followSystem = settings.theme == ThemeMode.SYSTEM, reduceMotion = settings.reduceMotion) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val player by app.player.state.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        val pendingOpen by openRequests.collectAsStateWithLifecycle()
        val tracking by app.tracking.state.collectAsStateWithLifecycle()

        LaunchedEffect(pendingOpen, route) {
            if (pendingOpen != null && route != null && route != Routes.SPLASH) {
                if (tracking.active && route != Routes.TRACKING) nav.navigate(Routes.TRACKING) { launchSingleTop = true }
                openRequests.value = null
            }
        }

        val c = AppTheme.colors
        // System bar icons follow the app theme (Midnight is always dark), not the device setting.
        val activity = LocalActivity.current as? ComponentActivity
        LaunchedEffect(c.isDark) {
            val style = if (c.isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        CompositionLocalProvider(LocalSnackbar provides snackbar) {
            Scaffold(
                containerColor = c.background,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (route in Routes.tabs) {
                        Column {
                            if (player.hasContent) {
                                MiniPlayer(
                                    player, onOpen = { nav.navigate(Routes.PLAYER) },
                                    onToggle = { app.player.toggle() }, onClose = { app.player.stop() },
                                )
                            }
                            NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
                                tabs.forEach { t ->
                                    NavigationBarItem(
                                        selected = route == t.route,
                                        onClick = { nav.switchTab(t.route) },
                                        icon = { androidx.compose.material3.Icon(painterResource(t.icon), null) },
                                        label = { Text(t.label, style = MaterialTheme.typography.bodySmall) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = c.primary, selectedTextColor = c.primary,
                                            indicatorColor = c.elevated, unselectedIconColor = c.muted, unselectedTextColor = c.muted,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { inner ->
                NavHost(
                    nav, startDestination = Routes.SPLASH,
                    modifier = Modifier.padding(inner).consumeWindowInsets(inner),
                    enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
                ) {
                    // Onboarding
                    composable(Routes.SPLASH) { SplashScreen(app, nav) }
                    composable(Routes.ONBOARDING) { OnboardingScreen(nav) }
                    composable(Routes.PERM_MIC) { MicPermissionScreen(nav) }
                    composable(Routes.PERM_NOTIF) { NotificationPermissionScreen(nav) }
                    composable(Routes.GOAL_SETUP) { GoalSetupScreen(app, nav) }
                    // Account (optional)
                    composable(Routes.LOGIN) { LoginScreen(app, nav) }
                    composable(Routes.SIGNUP) { SignUpScreen(app, nav) }
                    composable(Routes.FORGOT) { ForgotPasswordScreen(app, nav) }
                    composable(Routes.VERIFY) { VerifyCodeScreen(app, nav) }
                    composable(Routes.NEW_PASSWORD) { NewPasswordScreen(app, nav) }
                    composable(Routes.RESET_DONE) { ResetSuccessScreen(nav) }
                    // Home & discovery
                    composable(Routes.HOME) { HomeScreen(app, nav) }
                    composable(Routes.NOTIFICATIONS) { NotificationsScreen(app, nav) }
                    composable(Routes.SEARCH) { SearchScreen(app, nav) }
                    composable(Routes.RECOMMENDATIONS) { RecommendationsScreen(app, nav) }
                    // Sounds
                    composable(Routes.LIBRARY) { LibraryScreen(app, nav) }
                    composable(Routes.CATEGORY, listOf(navArgument("cat") { type = NavType.StringType })) {
                        CategoryScreen(app, nav, it.arguments?.getString("cat").orEmpty())
                    }
                    composable(Routes.SOUND, listOf(navArgument("id") { type = NavType.StringType })) {
                        SoundDetailsScreen(app, nav, it.arguments?.getString("id").orEmpty())
                    }
                    composable(Routes.PLAYER) { PlayerScreen(app, nav) }
                    composable(Routes.TIMER) { SleepTimerScreen(app, nav) }
                    composable(Routes.MIXER) { MixerScreen(app, nav) }
                    composable(Routes.ADD_SOUND) { AddSoundScreen(app, nav) }
                    composable(Routes.SAVE_MIX) { SaveMixScreen(app, nav) }
                    composable(Routes.MIX_SAVED) { MixSavedScreen(nav) }
                    composable(Routes.MIXES) { MyMixesScreen(app, nav) }
                    composable(Routes.FAVORITES) { FavoritesScreen(app, nav) }
                    // Sleep tracking
                    composable(Routes.SLEEP) { SleepDashboardScreen(app, nav) }
                    composable(Routes.SESSION_SETUP) { SessionSetupScreen(app, nav) }
                    composable(Routes.ALARM) { WakeAlarmScreen(app, nav) }
                    composable(Routes.MIC_TEST) { MicTestScreen(nav) }
                    composable(Routes.PLACEMENT) { PlacementScreen(app, nav) }
                    composable(Routes.TRACKING) { ActiveTrackingScreen(app, nav) }
                    composable(Routes.ANALYZING) { AnalyzingScreen(app, nav) }
                    composable(Routes.PERM_DENIED) { PermissionDeniedScreen(app, nav) }
                    composable(Routes.TRACKING_ERROR) { TrackingErrorScreen(app, nav) }
                    // Insights & reports
                    composable(Routes.SUMMARY) { MorningSummaryScreen(app, nav, it.arguments?.getString("id").orEmpty()) }
                    composable(Routes.INSIGHTS) { InsightsScreen(app, nav) }
                    composable(Routes.REPORT) { DailyReportScreen(app, nav, it.arguments?.getString("id").orEmpty()) }
                    composable(Routes.WEEKLY) { WeeklyReportScreen(app, nav) }
                    composable(Routes.MONTHLY) { MonthlyReportScreen(app, nav) }
                    composable(Routes.HISTORY) { HistoryScreen(app, nav) }
                    composable(Routes.TIMELINE) { TimelineScreen(app, nav, it.arguments?.getString("id").orEmpty()) }
                    composable(Routes.EVENT) {
                        EventDetailsScreen(app, nav, it.arguments?.getString("id").orEmpty(), it.arguments?.getString("index")?.toIntOrNull() ?: 0)
                    }
                    composable(Routes.GOALS) { SleepGoalsScreen(app, nav) }
                    // Profile & settings
                    composable(Routes.PROFILE) { ProfileScreen(app, nav) }
                    composable(Routes.EDIT_PROFILE) { EditProfileScreen(app, nav) }
                    composable(Routes.SETTINGS) { SettingsScreen(nav) }
                    composable(Routes.AUDIO_SETTINGS) { AudioSettingsScreen(app, nav) }
                    composable(Routes.TRACKING_SETTINGS) { TrackingSettingsScreen(app, nav) }
                    composable(Routes.REMINDER_SETTINGS) { ReminderSettingsScreen(app, nav) }
                    composable(Routes.APPEARANCE) { AppearanceScreen(app, nav) }
                    composable(Routes.PRIVACY) { PrivacyScreen(app, nav) }
                    composable(Routes.FAQ) { FaqScreen(nav) }
                    composable(Routes.PREMIUM) { PremiumScreen(nav) }
                }
            }
        }
    }
}
