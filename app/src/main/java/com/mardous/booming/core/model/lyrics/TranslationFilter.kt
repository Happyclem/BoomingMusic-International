package com.mardous.booming.core.model.lyrics

import androidx.compose.runtime.Immutable
import com.mardous.booming.data.model.lyrics.SyncedLyrics

/**
 * Which translation layers should be displayed for the current lyrics.
 *
 * - [All] keeps every translation stacked (the default; matches the multiple-translations behavior).
 * - [Off] hides every translation, showing only the original.
 * - [Language] shows only the layers whose language code matches [lang].
 */
@Immutable
sealed interface TranslationFilter {

    data object All : TranslationFilter
    data object Off : TranslationFilter
    data class Language(val lang: String) : TranslationFilter

    /**
     * Filters a line's translation layers according to this selection.
     */
    fun apply(translations: List<SyncedLyrics.Translation>): List<SyncedLyrics.Translation> =
        when (this) {
            All -> translations
            Off -> emptyList()
            is Language -> translations.filter { it.lang == lang }
        }

    companion object {
        /** Serialized form persisted in preferences. */
        const val VALUE_ALL = "all"
        const val VALUE_OFF = "off"

        /**
         * Human-readable display name for a language code (e.g. "fr" -> "Français"), falling back
         * to the raw code when it isn't recognized.
         */
        fun displayLanguage(lang: String): String {
            val locale = java.util.Locale.forLanguageTag(lang)
            return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
                .ifBlank { lang }
        }

        fun fromValue(value: String?): TranslationFilter = when (value) {
            null, VALUE_ALL -> All
            VALUE_OFF -> Off
            else -> Language(value)
        }

        fun TranslationFilter.toValue(): String = when (this) {
            All -> VALUE_ALL
            Off -> VALUE_OFF
            is Language -> lang
        }
    }
}
