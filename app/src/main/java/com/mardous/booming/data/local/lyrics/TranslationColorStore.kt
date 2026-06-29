package com.mardous.booming.data.local.lyrics

import android.content.SharedPreferences
import androidx.core.content.edit
import com.mardous.booming.core.model.lyrics.TranslationColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists, in [SharedPreferences], the per-language translation colors chosen by the user and the
 * set of language codes seen across opened lyric files (so the settings screen can list only the
 * languages that actually appear in the user's library).
 *
 * Storage format:
 * - [KEY_DETECTED]: a set of lowercase language codes.
 * - [KEY_COLORS]: a set of "lang|aarrggbb" entries (color as a hex ARGB int).
 */
class TranslationColorStore(private val preferences: SharedPreferences) {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<TranslationColors> = _state.asStateFlow()

    private fun load(): TranslationColors {
        val detected = preferences.getStringSet(KEY_DETECTED, emptySet()).orEmpty()
        val colors = preferences.getStringSet(KEY_COLORS, emptySet()).orEmpty()
            .mapNotNull { entry ->
                val sep = entry.indexOf('|')
                if (sep <= 0) return@mapNotNull null
                val lang = entry.substring(0, sep)
                val color = entry.substring(sep + 1).toIntOrNull() ?: return@mapNotNull null
                lang to color
            }
            .toMap()
        return TranslationColors(colors = colors, detectedLanguages = detected.sorted())
    }

    private fun persist(state: TranslationColors) {
        preferences.edit {
            putStringSet(KEY_DETECTED, state.detectedLanguages.toSet())
            putStringSet(KEY_COLORS, state.colors.map { (lang, color) -> "$lang|$color" }.toSet())
        }
        _state.value = state
    }

    /** Records languages seen in a freshly parsed file, adding any new ones to the detected set. */
    fun onLanguagesDetected(languages: Collection<String>) {
        if (languages.isEmpty()) return
        val normalized = languages.map { it.lowercase() }
        val current = _state.value
        if (current.detectedLanguages.containsAll(normalized)) return
        val merged = (current.detectedLanguages + normalized).distinct().sorted()
        persist(current.copy(detectedLanguages = merged))
    }

    /** Sets (or clears, when [color] is null) the color for a language. */
    fun setColor(lang: String, color: Int?) {
        val key = lang.lowercase()
        val current = _state.value
        val newColors = current.colors.toMutableMap().apply {
            if (color == null) remove(key) else put(key, color)
        }
        persist(current.copy(colors = newColors))
    }

    companion object {
        private const val KEY_DETECTED = "lyrics_detected_translation_languages"
        private const val KEY_COLORS = "lyrics_translation_colors"
    }
}
