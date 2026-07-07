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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Blocking DAO for the file-tag cache. Methods are non-suspend on purpose: they
 * are called from the (already background-dispatched) song cursor mapping, mirroring
 * how [InclExclDao.blackListPaths] is used synchronously by the repository.
 */
@Dao
interface SongTagCacheDao {
    @Query("SELECT * FROM SongTagCacheEntity WHERE id = :id")
    fun get(id: Long): SongTagCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: SongTagCacheEntity)

    @Query("DELETE FROM SongTagCacheEntity")
    fun clear()
}
