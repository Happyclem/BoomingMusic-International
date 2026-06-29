package com.mardous.booming.core.model.lyrics

import androidx.compose.runtime.Immutable

/**
 * The user-defined color rules for translation languages, plus the set of language codes that have
 * been seen across opened lyric files (used to populate the settings list).
 *
 * @param colors maps a lowercase BCP-47 language code to a packed ARGB color int.
 * @param detectedLanguages every language code encountered so far, in first-seen order.
 */
@Immutable
data class TranslationColors(
    val colors: Map<String, Int> = emptyMap(),
    val detectedLanguages: List<String> = emptyList()
) {
    /** The packed ARGB color chosen for [lang], or null when the user hasn't set one. */
    fun colorFor(lang: String?): Int? = lang?.let { colors[it.lowercase()] }
}
