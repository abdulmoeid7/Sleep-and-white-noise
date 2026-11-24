package com.example.sleepandwhitenoise.data

import androidx.annotation.DrawableRes
import com.example.sleepandwhitenoise.R

enum class SoundCategory(val title: String, val blurb: String) {
    NATURE("Nature", "Rain, waves and forest air"),
    NOISE("Noise", "Steady colored noise for focus and sleep"),
    COZY("Cozy", "Warm indoor ambience"),
}

/** How the [com.example.sleepandwhitenoise.audio.NoiseEngine] synthesizes a sound. */
enum class Synth { WHITE, PINK, BROWN, RAIN, OCEAN, FOREST, FIREPLACE, WIND }

data class Sound(
    val id: String,
    val name: String,
    val description: String,
    val category: SoundCategory,
    @DrawableRes val image: Int,
    val synth: Synth,
    /** Lower is more popular; used by the "Most popular" sort. */
    val popularity: Int,
)

/**
 * The design pack ships no audio files ("Audio assets must be original or licensed"), so every
 * sound here is generated on-device in real time. Nothing needs downloading: all sounds work offline.
 */
object SoundCatalog {
    val all: List<Sound> = listOf(
        Sound("rain", "Gentle rain", "A soft, steady rainfall.", SoundCategory.NATURE, R.drawable.img_rain, Synth.RAIN, 1),
        Sound("ocean", "Ocean waves", "Slow waves rolling onto the shore.", SoundCategory.NATURE, R.drawable.img_ocean, Synth.OCEAN, 2),
        Sound("brown", "Brown noise", "Deep, rumbling noise that masks low sounds.", SoundCategory.NOISE, R.drawable.img_midnight, Synth.BROWN, 3),
        Sound("white", "White noise", "Even, full-spectrum noise.", SoundCategory.NOISE, R.drawable.img_noise, Synth.WHITE, 4),
        Sound("forest", "Forest ambience", "Breezy treetops with distant birds.", SoundCategory.NATURE, R.drawable.img_forest, Synth.FOREST, 5),
        Sound("fireplace", "Fireplace", "A low fire with gentle crackles.", SoundCategory.COZY, R.drawable.img_fireplace, Synth.FIREPLACE, 6),
        Sound("pink", "Pink noise", "Balanced noise, softer than white.", SoundCategory.NOISE, R.drawable.img_noise, Synth.PINK, 7),
        Sound("wind", "Night wind", "A calm breeze rising and falling.", SoundCategory.NATURE, R.drawable.img_midnight, Synth.WIND, 8),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Sound? = byId[id]
    fun require(id: String): Sound = byId[id] ?: all.first()

    fun search(query: String): List<Sound> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return all.filter {
            it.name.lowercase().contains(q) || it.category.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
        }
    }

    val popularSearches = listOf("Rain", "Ocean waves", "White noise", "Fireplace")
}
