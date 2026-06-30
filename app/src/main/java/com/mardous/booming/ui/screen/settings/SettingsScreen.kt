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

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
enum class SettingsScreen(
    @LayoutRes val layoutRes: Int,
    @IdRes val navAction: Int,
    @IdRes val searchNavAction: Int,
    @StringRes val titleRes: Int
) {
    Appearance(R.xml.preferences_screen_appearance, R.id.action_to_appearancePreferences, R.id.action_search_to_appearancePreferences, R.string.appearance_title),
    NowPlaying(R.xml.preferences_screen_now_playing, R.id.action_to_nowPlayingPreferences, R.id.action_search_to_nowPlayingPreferences, R.string.now_playing_title),
    Lyrics(R.xml.preferences_screen_lyrics, R.id.action_to_lyricsPreferences, R.id.action_search_to_lyricsPreferences, R.string.lyrics_preferences_title),
    Playback(R.xml.preferences_screen_playback, R.id.action_to_playbackPreferences, R.id.action_search_to_playbackPreferences, R.string.playback_title),
    Library(R.xml.preferences_screen_library, R.id.action_to_libraryPreferences, R.id.action_search_to_libraryPreferences, R.string.library_title),
    Network(R.xml.preferences_screen_network, R.id.action_to_networkPreferences, R.id.action_search_to_networkPreferences, R.string.network_title),
    Advanced(R.xml.preferences_screen_advanced, R.id.action_to_advancedPreferences, R.id.action_search_to_advancedPreferences, R.string.advanced_title);
}