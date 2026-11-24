package com.example.sleepandwhitenoise

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sleepandwhitenoise.audio.PlayerController
import com.example.sleepandwhitenoise.data.AuthRepository
import com.example.sleepandwhitenoise.data.SessionStore
import com.example.sleepandwhitenoise.data.SettingsRepository
import com.example.sleepandwhitenoise.tracking.TrackingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Simple service locator: app-wide singletons shared by screens and services. */
class SleepApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var settings: SettingsRepository private set
    lateinit var sessions: SessionStore private set
    lateinit var player: PlayerController private set
    lateinit var tracking: TrackingController private set
    lateinit var auth: AuthRepository private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this, scope)
        sessions = SessionStore(this, scope)
        player = PlayerController(this, scope)
        tracking = TrackingController(this)
        auth = AuthRepository(settings)
        Notifications.createChannels(this)

        scope.launch {
            combine(settings.loaded, sessions.loaded) { a, b -> a && b }.first { it }
            val s = settings.current
            player.setDefaultVolume(s.defaultVolume)
            sessions.pruneOlderThan(s.retentionDays)
        }

        // "Background playback • Off" pauses sounds when the app leaves the screen.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (!settings.current.backgroundPlayback && player.state.value.playing) player.pause()
            }
        })
    }

    fun whenSettingsLoaded(block: () -> Unit) {
        scope.launch {
            settings.loaded.first { it }
            block()
        }
    }

    companion object {
        fun get(ctx: Context): SleepApp = ctx.applicationContext as SleepApp
    }
}
