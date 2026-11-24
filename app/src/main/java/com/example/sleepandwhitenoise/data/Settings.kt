package com.example.sleepandwhitenoise.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

enum class Repeat(val label: String) { ONCE("Once"), WEEKDAYS("Weekdays"), DAILY("Daily") }
enum class AlarmTone(val label: String) { SOFT_CHIME("Soft chime"), DEVICE_ALARM("Device alarm tone") }
enum class ThemeMode(val label: String) { MIDNIGHT("Midnight dark"), SYSTEM("System theme") }

data class MixLayer(val soundId: String, val volume: Float)
data class Mix(val id: String, val name: String, val layers: List<MixLayer>)

data class AppSettings(
    val onboarded: Boolean = false,
    val displayName: String = "",
    val accountEmail: String? = null,
    // Sleep goal
    val sleepTargetMin: Int = 8 * 60,
    val bedtimeMin: Int = 22 * 60 + 30,
    val wakeMin: Int = 6 * 60 + 30,
    // Wake-up alarm
    val alarmEnabled: Boolean = true,
    val alarmMin: Int = 6 * 60 + 30,
    val alarmRepeat: Repeat = Repeat.WEEKDAYS,
    val alarmTone: AlarmTone = AlarmTone.SOFT_CHIME,
    val alarmVibrate: Boolean = true,
    // Sleep tracking
    val soundMonitoring: Boolean = true,
    val relaxationSoundId: String? = "rain",
    val saveClips: Boolean = false,
    val retentionDays: Int = 30,
    // Audio
    val defaultVolume: Float = 0.6f,
    val fadeOutMin: Int = 5,
    val backgroundPlayback: Boolean = true,
    // Reminders
    val reminderEnabled: Boolean = true,
    val reminderMin: Int = 22 * 60,
    val reminderRepeat: Repeat = Repeat.DAILY,
    // Appearance
    val theme: ThemeMode = ThemeMode.MIDNIGHT,
    val reduceMotion: Boolean = false,
    // Library
    val favorites: Set<String> = emptySet(),
    val recentSearches: List<String> = emptyList(),
    val mixes: List<Mix> = emptyList(),
) {
    val profileName: String get() = displayName.ifBlank { "Guest" }
    val signedIn: Boolean get() = accountEmail != null
}

private val Context.dataStore by preferencesDataStore(name = "settings")
private val KEY = stringPreferencesKey("settings_json")

/** Local-only settings store. The whole settings object is persisted atomically as JSON. */
class SettingsRepository(private val context: Context, private val scope: CoroutineScope) {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val mutex = Mutex()

    init {
        scope.launch {
            _settings.value = context.dataStore.data.map { p -> p[KEY]?.let(::decode) ?: AppSettings() }.first()
            _loaded.value = true
        }
    }

    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        scope.launch {
            mutex.withLock { context.dataStore.edit { it[KEY] = encode(next) } }
        }
    }

    fun toggleFavorite(id: String) = update {
        it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id)
    }

    fun addRecentSearch(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        update { s -> s.copy(recentSearches = (listOf(t) + s.recentSearches.filterNot { it.equals(t, true) }).take(5)) }
    }

    fun saveMix(name: String, layers: List<MixLayer>): Mix {
        val mix = Mix(System.currentTimeMillis().toString(), name.trim(), layers)
        update { it.copy(mixes = listOf(mix) + it.mixes) }
        return mix
    }

    fun deleteMix(id: String) = update { s -> s.copy(mixes = s.mixes.filterNot { it.id == id }) }

    private fun encode(s: AppSettings): String = JSONObject().apply {
        put("onboarded", s.onboarded); put("displayName", s.displayName)
        put("accountEmail", s.accountEmail ?: JSONObject.NULL)
        put("sleepTargetMin", s.sleepTargetMin); put("bedtimeMin", s.bedtimeMin); put("wakeMin", s.wakeMin)
        put("alarmEnabled", s.alarmEnabled); put("alarmMin", s.alarmMin)
        put("alarmRepeat", s.alarmRepeat.name); put("alarmTone", s.alarmTone.name); put("alarmVibrate", s.alarmVibrate)
        put("soundMonitoring", s.soundMonitoring); put("relaxationSoundId", s.relaxationSoundId ?: JSONObject.NULL)
        put("saveClips", s.saveClips); put("retentionDays", s.retentionDays)
        put("defaultVolume", s.defaultVolume.toDouble()); put("fadeOutMin", s.fadeOutMin)
        put("backgroundPlayback", s.backgroundPlayback)
        put("reminderEnabled", s.reminderEnabled); put("reminderMin", s.reminderMin); put("reminderRepeat", s.reminderRepeat.name)
        put("theme", s.theme.name); put("reduceMotion", s.reduceMotion)
        put("favorites", JSONArray(s.favorites.toList()))
        put("recentSearches", JSONArray(s.recentSearches))
        put("mixes", JSONArray().apply {
            s.mixes.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("name", m.name)
                    put("layers", JSONArray().apply {
                        m.layers.forEach { l -> put(JSONObject().put("soundId", l.soundId).put("volume", l.volume.toDouble())) }
                    })
                })
            }
        })
    }.toString()

    private fun decode(raw: String): AppSettings = runCatching {
        val o = JSONObject(raw)
        val d = AppSettings()
        fun strings(key: String) = o.optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
        AppSettings(
            onboarded = o.optBoolean("onboarded", d.onboarded),
            displayName = o.optString("displayName", d.displayName),
            accountEmail = if (o.isNull("accountEmail")) null else o.optString("accountEmail"),
            sleepTargetMin = o.optInt("sleepTargetMin", d.sleepTargetMin),
            bedtimeMin = o.optInt("bedtimeMin", d.bedtimeMin),
            wakeMin = o.optInt("wakeMin", d.wakeMin),
            alarmEnabled = o.optBoolean("alarmEnabled", d.alarmEnabled),
            alarmMin = o.optInt("alarmMin", d.alarmMin),
            alarmRepeat = enumOr(o.optString("alarmRepeat"), d.alarmRepeat),
            alarmTone = enumOr(o.optString("alarmTone"), d.alarmTone),
            alarmVibrate = o.optBoolean("alarmVibrate", d.alarmVibrate),
            soundMonitoring = o.optBoolean("soundMonitoring", d.soundMonitoring),
            relaxationSoundId = if (o.isNull("relaxationSoundId")) null else o.optString("relaxationSoundId", "rain"),
            saveClips = o.optBoolean("saveClips", d.saveClips),
            retentionDays = o.optInt("retentionDays", d.retentionDays),
            defaultVolume = o.optDouble("defaultVolume", d.defaultVolume.toDouble()).toFloat(),
            fadeOutMin = o.optInt("fadeOutMin", d.fadeOutMin),
            backgroundPlayback = o.optBoolean("backgroundPlayback", d.backgroundPlayback),
            reminderEnabled = o.optBoolean("reminderEnabled", d.reminderEnabled),
            reminderMin = o.optInt("reminderMin", d.reminderMin),
            reminderRepeat = enumOr(o.optString("reminderRepeat"), d.reminderRepeat),
            theme = enumOr(o.optString("theme"), d.theme),
            reduceMotion = o.optBoolean("reduceMotion", d.reduceMotion),
            favorites = strings("favorites").toSet(),
            recentSearches = strings("recentSearches"),
            mixes = o.optJSONArray("mixes")?.let { a ->
                List(a.length()) { i ->
                    val m = a.getJSONObject(i)
                    val la = m.getJSONArray("layers")
                    Mix(m.getString("id"), m.getString("name"), List(la.length()) { j ->
                        val l = la.getJSONObject(j)
                        MixLayer(l.getString("soundId"), l.getDouble("volume").toFloat())
                    })
                }
            } ?: emptyList(),
        )
    }.getOrDefault(AppSettings())

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        runCatching { enumValueOf<E>(name) }.getOrDefault(default)
}
