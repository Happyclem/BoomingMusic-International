/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.settings

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import java.text.Normalizer

/**
 * A single searchable preference, flattened from the per-screen preference trees.
 *
 * @param screen the settings sub-screen this preference lives in
 * @param key the preference key (used to scroll to and highlight it on arrival)
 * @param title the preference title shown to the user
 * @param summary the preference summary, if any
 * @param categoryTitle the title of the [PreferenceCategory] this preference belongs to, if any
 */
data class SettingsSearchEntry(
    val screen: SettingsScreen,
    val key: String,
    val title: String,
    val summary: String?,
    val categoryTitle: String?
) {
    private val haystack: String =
        normalize(listOfNotNull(title, summary, categoryTitle).joinToString(" "))

    fun matches(normalizedQuery: String): Boolean = haystack.contains(normalizedQuery)

    companion object {
        /**
         * Lower-cases and strips diacritics so that, e.g., "themes" matches "Thèmes".
         */
        fun normalize(text: CharSequence): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase()

        private val DIACRITICS = "\\p{Mn}+".toRegex()
    }
}

/**
 * Builds and queries the searchable index of every leaf preference across all settings sub-screens.
 *
 * The index is built by inflating each screen's preference XML into a temporary tree and walking it,
 * which lets the framework resolve titles/summaries (including string resources) for us, so the
 * index stays in sync with the actual preferences without any duplicated metadata.
 */
class SettingsSearchIndex(context: Context) {

    val entries: List<SettingsSearchEntry> by lazy { buildIndex(context) }

    fun search(query: String?): List<SettingsSearchEntry> {
        val trimmed = query?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()
        val normalized = SettingsSearchEntry.normalize(trimmed)
        return entries.filter { it.matches(normalized) }
    }

    private fun buildIndex(context: Context): List<SettingsSearchEntry> {
        val manager = PreferenceManager(context)
        return SettingsScreen.entries.flatMap { screen ->
            val root = manager.inflateFromResource(context, screen.layoutRes, null)
            buildList { collect(screen, root, null, this) }
        }
    }

    private fun collect(
        screen: SettingsScreen,
        group: PreferenceGroup,
        categoryTitle: String?,
        output: MutableList<SettingsSearchEntry>
    ) {
        for (i in 0 until group.preferenceCount) {
            val preference = group.getPreference(i)
            if (preference is PreferenceGroup) {
                val childCategory = (preference as? PreferenceCategory)?.title?.toString() ?: categoryTitle
                collect(screen, preference, childCategory, output)
            } else {
                val key = preference.key
                val title = preference.title?.toString()
                if (key.isNullOrEmpty() || title.isNullOrEmpty()) continue
                output += SettingsSearchEntry(
                    screen = screen,
                    key = key,
                    title = title,
                    summary = preference.summary?.toString(),
                    categoryTitle = categoryTitle
                )
            }
        }
    }
}
