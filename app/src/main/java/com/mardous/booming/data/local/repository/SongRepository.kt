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

package com.mardous.booming.data.local.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.OpenableColumns
import android.util.Log
import androidx.media3.common.MediaItem
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.data.local.room.InclExclEntity
import com.mardous.booming.data.local.room.SongTagCacheDao
import com.mardous.booming.data.local.room.SongTagCacheEntity
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.utilities.getStringSafe
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.util.Preferences
import okhttp3.internal.toLongOrDefault

interface SongRepository {
    fun songs(): List<Song>
    fun songs(query: String): List<Song>
    fun songs(cursor: Cursor?): List<Song>
    suspend fun songsByUri(uri: Uri): List<Song>
    suspend fun songsByMediaItems(mediaItems: List<MediaItem>): Pair<List<Song>, List<MediaItem>>
    suspend fun songByMediaItem(mediaItem: MediaItem?): Song
    fun songByFilePath(filePath: String, ignoreBlacklist: Boolean = false): Song
    fun song(cursor: Cursor?): Song
    fun song(songId: Long): Song
    suspend fun initializeBlacklist()
}

@SuppressLint("InlinedApi")
class RealSongRepository(
    private val context: Context,
    private val inclExclDao: InclExclDao,
    private val songTagCacheDao: SongTagCacheDao
) : SongRepository {

    override fun songs(): List<Song> {
        val songs = songs(makeSongCursor(null, null))
        return with(SongSortMode.AllSongs) { songs.sorted() }
    }

    override fun songs(query: String): List<Song> {
        return songs(
            makeSongCursor(
                selection = "${AudioColumns.TITLE} LIKE ? OR ${AudioColumns.ARTIST} LIKE ? OR ${AudioColumns.ALBUM} LIKE ?",
                selectionValues = arrayOf("%$query%", "%$query%", "%$query%")
            )
        )
    }

    override fun songs(cursor: Cursor?): List<Song> {
        return cursor.use {
            it.mapIfValid { getSongFromCursorImpl(this) }
        }
    }

    override fun song(cursor: Cursor?): Song {
        return cursor.use {
            it.takeOrDefault(Song.emptySong) { getSongFromCursorImpl(this) }
        }
    }

    override fun song(songId: Long): Song {
        return song(makeSongCursor("${AudioColumns._ID}=?", arrayOf(songId.toString())))
    }

    override suspend fun songsByUri(uri: Uri): List<Song> {
        var songs: List<Song> = emptyList()
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val authority = uri.authority ?: ""
            when (authority) {
                MediaStore.AUTHORITY -> {
                    val songId = uri.lastPathSegment?.toLongOrNull()
                    if (songId != null) {
                        songs = listOf(song(songId))
                    }
                }

                else -> {
                    try {
                        if (hasQ()) {
                            val context = context.applicationContext
                            val id = MediaStore.getMediaUri(context, uri)
                                ?.lastPathSegment?.toLongOrNull()
                            if (id != null) {
                                songs = listOf(song(id))
                            }
                        } else {
                            if (authority == "com.android.providers.media.documents") {
                                val id = getSongIdFromMediaProvider(uri)
                                if (id > -1) {
                                    songs = listOf(song(id))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to retrieve song info from Uri: $uri", e)
                    }
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path
            if (path != null) {
                songs = listOf(songByFilePath(path, true))
            }
        }

        if (songs.isEmpty() && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            songs = listOf(findSongFromFileProviderUri(uri))
        }

        if (songs.isEmpty()) {
            Log.e(TAG, "Couldn't resolve songs from Uri: $uri")
        }

        return songs
    }

    override suspend fun songsByMediaItems(mediaItems: List<MediaItem>): Pair<List<Song>, List<MediaItem>> {
        if (mediaItems.isEmpty()) return (emptyList<Song>() to mediaItems)

        val ids = mediaItems.map { it.mediaId }
        val allSongs = buildList {
            ids.chunked(900).forEach { chunk ->
                val selection = "${AudioColumns._ID} IN (${chunk.joinToString(",") { "?" }})"
                val selectionArgs = chunk.toTypedArray()
                addAll(songs(makeSongCursor(selection = selection, selectionValues = selectionArgs)))
            }
        }

        val songMap = allSongs.associateBy { it.id.toString() }
        val (found, missing) = mediaItems.partition { item ->
            songMap[item.mediaId]?.takeIf { it != Song.emptySong } != null
        }

        val resultSongs = found.mapNotNull { songMap[it.mediaId] }
        return resultSongs to missing
    }

    override suspend fun songByMediaItem(mediaItem: MediaItem?): Song {
        if (mediaItem != null) {
            var song = mediaItem.localConfiguration?.tag
            if (song == null || song !is Song) {
                song = song(
                    makeSongCursor(
                        selection = "${AudioColumns._ID}=?",
                        selectionValues = arrayOf(mediaItem.mediaId)
                    )
                )
            }
            return song
        }
        return Song.emptySong
    }

    override fun songByFilePath(filePath: String, ignoreBlacklist: Boolean): Song {
        return song(
            makeSongCursor(
                selection = "${AudioColumns.DATA}=?",
                selectionValues = arrayOf(filePath),
                ignoreBlacklist = ignoreBlacklist
            )
        )
    }

    override suspend fun initializeBlacklist() {
        val excludedPaths = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
        )
        for (path in excludedPaths) {
            inclExclDao.insertPath(InclExclEntity(path.getCanonicalPathSafe(), InclExclDao.BLACKLIST))
        }
    }

    fun makeSongCursor(queryDispatcher: MediaQueryDispatcher, ignoreBlacklist: Boolean = false): Cursor? {
        val minimumSongDuration = Preferences.minimumSongDuration
        if (minimumSongDuration > 0) {
            queryDispatcher.addSelection("${AudioColumns.DURATION} >= ${minimumSongDuration * 1000}")
        }

        if (!ignoreBlacklist) {
            // Whitelist
            if (Preferences.whitelistEnabled) {
                val whitelisted = inclExclDao.whitelistPaths().map { it.path }
                if (whitelisted.isNotEmpty()) {
                    queryDispatcher.addSelection(generateWhitelistSelection(whitelisted.size))
                    queryDispatcher.addArguments(*addLibrarySelectionValues(whitelisted))
                }
            }

            // Blacklist
            if (Preferences.blacklistEnabled) {
                val blacklisted = inclExclDao.blackListPaths().map { it.path }
                if (blacklisted.isNotEmpty()) {
                    queryDispatcher.addSelection(generateBlacklistSelection(blacklisted.size))
                    queryDispatcher.addArguments(*addLibrarySelectionValues(blacklisted))
                }
            }
        }

        return try {
            queryDispatcher.dispatch()
        } catch (e: SecurityException) {
            Log.e(TAG, "Couldn't load songs", e)
            null
        }
    }

    fun makeSongCursor(
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String? = null,
        ignoreBlacklist: Boolean = false
    ): Cursor? {
        val queryDispatcher = MediaQueryDispatcher()
            .setProjection(getBaseProjection())
            .setSelection(BASE_SELECTION)
            .setSelectionArguments(selectionValues)
            .addSelection(selection)
            .setSortOrder(sortOrder ?: MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        return makeSongCursor(queryDispatcher, ignoreBlacklist)
    }

    private fun generateWhitelistSelection(pathCount: Int): String =
        buildString {
            append("(")
            append((1..pathCount).joinToString(" OR ") { "${AudioColumns.DATA} LIKE ?" })
            append(")")
        }

    private fun generateBlacklistSelection(pathCount: Int): String =
        (1..pathCount).joinToString(" AND ") { "${AudioColumns.DATA} NOT LIKE ?" }


    private fun addLibrarySelectionValues(paths: List<String>): Array<String> {
        return Array(paths.size) { index -> "${paths[index]}%" }
    }

    private fun getSongIdFromMediaProvider(uri: Uri): Long {
        val docId = DocumentsContract.getDocumentId(uri)
        val parts = docId.split(":")
        return if (parts.size == 2) parts[1].toLongOrDefault(-1) else -1
    }

    private fun getDisplayNameAndSize(uri: Uri): Pair<String, Long>? {
        return MediaQueryDispatcher(uri)
            .withColumns(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            .dispatch()?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name =
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    return name to size
                } else null
            }
    }

    private fun findSongFromFileProviderUri(uri: Uri): Song {
        val (name, size) = getDisplayNameAndSize(uri)
            ?: return Song.emptySong

        val selection = "${AudioColumns.DISPLAY_NAME} = ? AND ${AudioColumns.SIZE} = ?"
        val selectionArgs = arrayOf(name, size.toString())

        val cursor = makeSongCursor(selection, selectionArgs, ignoreBlacklist = true)
        return song(cursor)
    }

    private fun getSongFromCursorImpl(cursor: Cursor): Song {
        val id = cursor.getLong(0)
        val data = cursor.getString(cursor.getColumnIndexOrThrow(AudioColumns.DATA))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(AudioColumns.TITLE))
        val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.TRACK))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.YEAR))
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.SIZE))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DURATION))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DATE_ADDED))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DATE_MODIFIED))
        val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID))
        val albumName = cursor.getStringSafe(AudioColumns.ALBUM) ?: Album.UNKNOWN_ALBUM_DISPLAY_NAME
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.ARTIST_ID))
        val artistName = cursor.getStringSafe(AudioColumns.ARTIST) ?: ""
        val albumArtistName = cursor.getStringSafe(AudioColumns.ALBUM_ARTIST)
        val genreName = cursor.getStringSafe(AudioColumns.GENRE)
        val volumeName = cursor.getStringSafe(AudioColumns.VOLUME_NAME)
        val song = Song(
            id,
            data,
            title,
            trackNumber,
            year,
            size,
            duration,
            dateAdded,
            dateModified,
            albumId,
            albumName,
            artistId,
            artistName,
            albumArtistName,
            genreName,
            volumeName
        )
        return if (Preferences.preferFileTags) enrichFromFileTags(song) else song
    }

    /**
     * Android's MediaStore scanner is unreliable at reading ID3v2.4 tags: fields
     * that are perfectly valid in the file (title, artist, year, …) can come back
     * empty or wrong (see mardous/BoomingMusic#178 and #25). TagLib parses both
     * ID3v2.3 and v2.4 correctly, so we read the display fields straight from the
     * file and let them override what MediaStore reported.
     *
     * Because this reads every file, the result is cached in Room keyed on the
     * MediaStore id and [Song.rawDateModified]; only new or changed files are read
     * from disk, so after the first scan subsequent library loads are cheap.
     *
     * Prototype notes:
     *  - The song list/album/artist grouping still uses MediaStore ids; only the
     *    displayed strings are overridden. Consistent tags group fine, but a fully
     *    tag-based grouping would be the next step.
     *  - The cache is looked up one row at a time; a batch pre-load keyed by the
     *    current cursor would cut per-song query overhead on very large libraries.
     */
    private fun enrichFromFileTags(song: Song): Song {
        // Reading tags and touching Room are blocking operations; never run them on
        // the main thread. Library loads are dispatched to IO, so the enrichment
        // still applies there — any stray main-thread lookup just keeps MediaStore's
        // values, exactly as before this feature existed.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return song
        }
        val cached = runCatching { songTagCacheDao.get(song.id) }.getOrNull()
        val tags = if (cached != null && cached.dateModified == song.rawDateModified) {
            cached
        } else {
            readAndCacheTags(song)
        }
        return tags?.applyTo(song) ?: song
    }

    private fun readAndCacheTags(song: Song): SongTagCacheEntity? {
        val reader = runCatching { MetadataReader(song.uri) }.getOrNull()
        if (reader == null || !reader.hasMetadata) {
            return null
        }
        val entity = SongTagCacheEntity(
            id = song.id,
            dateModified = song.rawDateModified,
            title = reader.value(MetadataReader.TITLE),
            artist = reader.value(MetadataReader.ARTIST),
            album = reader.value(MetadataReader.ALBUM),
            albumArtist = reader.value(MetadataReader.ALBUM_ARTIST),
            year = reader.first(MetadataReader.YEAR).parseYear(),
            genre = reader.genre()
        )
        runCatching { songTagCacheDao.upsert(entity) }
        return entity
    }

    /**
     * Overlays the cached file tags onto [song], preferring a tag value whenever it
     * is present and falling back to MediaStore's value otherwise. Returns [song]
     * unchanged when nothing differs, so healthy libraries keep object identity.
     */
    private fun SongTagCacheEntity.applyTo(song: Song): Song {
        val newTitle = title?.takeIf { it.isNotBlank() } ?: song.title
        val newArtist = artist?.takeIf { it.isNotBlank() } ?: song.artistName
        val newAlbum = album?.takeIf { it.isNotBlank() } ?: song.albumName
        val newAlbumArtist = albumArtist?.takeIf { it.isNotBlank() } ?: song.albumArtistName
        val newYear = if (year > 0) year else song.year
        val newGenre = genre?.takeIf { it.isNotBlank() } ?: song.genreName
        if (newTitle == song.title && newArtist == song.artistName && newAlbum == song.albumName &&
            newAlbumArtist == song.albumArtistName && newYear == song.year && newGenre == song.genreName
        ) {
            return song
        }
        return Song(
            song.id,
            song.data,
            newTitle,
            song.trackNumber,
            newYear,
            song.size,
            song.duration,
            song.dateAdded,
            song.rawDateModified,
            song.albumId,
            newAlbum,
            song.artistId,
            newArtist,
            newAlbumArtist,
            newGenre,
            song.volumeName
        )
    }

    /**
     * Extracts a 4-digit year from a tag date value. Handles a plain year
     * ("1997"), ISO dates ("1994-12-09") and day-first dates ("09-12-1994").
     */
    private fun String?.parseYear(): Int {
        if (isNullOrBlank()) return 0
        trim().toIntOrNull()?.let { if (it in 1000..9999) return it }
        return Regex("\\d{4}").find(this)?.value?.toIntOrNull()?.takeIf { it in 1000..9999 } ?: 0
    }

    companion object {
        private val TAG = RealSongRepository::class.java.simpleName

        const val BASE_SELECTION = "${AudioColumns.TITLE} != '' AND ${AudioColumns.IS_MUSIC} = 1"
        const val SEARCH_SELECTION = "${AudioColumns.TITLE} LIKE ? OR ${AudioColumns.ARTIST} LIKE ? OR ${AudioColumns.ALBUM} LIKE ?"

        @SuppressLint("InlinedApi")
        private val BASE_PROJECTION = arrayOf(
            AudioColumns._ID, //0
            AudioColumns.DATA, //1
            AudioColumns.TITLE, //2
            AudioColumns.TRACK, //3
            AudioColumns.YEAR, //4
            AudioColumns.SIZE, //5
            AudioColumns.DURATION, //6
            AudioColumns.DATE_ADDED, //7
            AudioColumns.DATE_MODIFIED, //8
            AudioColumns.ALBUM_ID, //9
            AudioColumns.ALBUM, //10
            AudioColumns.ARTIST_ID, //11
            AudioColumns.ARTIST, //12
            AudioColumns.ALBUM_ARTIST, //13
        )

        fun getAudioContentUri(): Uri = if (hasQ())
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        fun getBaseProjection(idColumn: String = AudioColumns._ID): Array<String> {
            var baseProjection = BASE_PROJECTION
            if (hasR()) {
                baseProjection += AudioColumns.GENRE
            }
            if (hasQ()) {
                baseProjection += AudioColumns.VOLUME_NAME
            }
            if (idColumn != AudioColumns._ID) {
                return baseProjection.copyOf().apply { set(0, idColumn) }
            }
            return baseProjection
        }

        fun generateSearchPattern(term: String, selection: String = SEARCH_SELECTION) =
            selection to Array(selection.count { it == '?' }) { "%$term%" }
    }
}