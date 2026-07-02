/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LyricsLinkDao {
    @Upsert
    suspend fun upsertLink(link: LyricsLinkEntity)

    @Query("SELECT * FROM LyricsLinkEntity WHERE id = :songId")
    suspend fun getLink(songId: Long): LyricsLinkEntity?

    @Query("SELECT * FROM LyricsLinkEntity")
    suspend fun getAllLinks(): List<LyricsLinkEntity>

    @Query("DELETE FROM LyricsLinkEntity WHERE id = :songId")
    suspend fun removeLink(songId: Long)

    @Query("DELETE FROM LyricsLinkEntity")
    suspend fun removeLinks()
}
