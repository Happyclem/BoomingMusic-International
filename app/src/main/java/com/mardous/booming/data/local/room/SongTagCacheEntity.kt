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

package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached file-tag values for a MediaStore song. Booming reads these fields
 * directly from the file with TagLib to work around MediaStore's unreliable
 * ID3v2.4 parsing (see mardous/BoomingMusic#178 and #25).
 *
 * Keyed by the MediaStore [id]; [dateModified] mirrors the file's modification
 * time so a stale row is transparently refreshed when the file changes.
 */
@Entity
class SongTagCacheEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    @ColumnInfo(name = "album_artist")
    val albumArtist: String?,
    val year: Int,
    val genre: String?
)
