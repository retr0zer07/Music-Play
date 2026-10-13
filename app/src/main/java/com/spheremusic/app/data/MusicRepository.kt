package com.spheremusic.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.spheremusic.app.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("sphere_music", Context.MODE_PRIVATE)

    fun favorites(): Set<Long> = preferences.getStringSet("favorites", emptySet())
        .orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun saveFavorites(ids: Set<Long>) {
        preferences.edit().putStringSet("favorites", ids.map { it.toString() }.toSet()).apply()
    }

    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
        )
        val result = mutableListOf<Song>()
        val cursor = context.contentResolver.query(
            collection, columns, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        ) ?: throw IOException("MediaStore returned no cursor")
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val albumId = it.getLong(4)
                fun label(index: Int, fallback: Int): String =
                    it.getString(index)?.takeUnless { value -> value.isBlank() || value == "<unknown>" }
                        ?: context.getString(fallback)
                result += Song(
                    id, ContentUris.withAppendedId(collection, id),
                    label(1, R.string.unknown_title), label(2, R.string.unknown_artist),
                    label(3, R.string.unknown_album), albumId, it.getLong(5).coerceAtLeast(0),
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId),
                )
            }
        }
        result
    }

    suspend fun lyrics(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val linked = preferences.getString("lyrics_${song.id}", null)
        if (linked != null) {
            readExternal(Uri.parse(linked))
        } else {
            val stream = context.contentResolver.openInputStream(song.uri)
                ?: throw IOException("Cannot open audio")
            stream.use { EmbeddedLyrics.read(it)?.let(LyricsParser::parse) }
        }
    }

    suspend fun linkLyrics(song: Song, uri: Uri): Lyrics = withContext(Dispatchers.IO) {
        val lyrics = readExternal(uri) ?: throw IllegalArgumentException("Empty lyrics")
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        preferences.edit().putString("lyrics_${song.id}", uri.toString()).apply()
        lyrics
    }

    private fun readExternal(uri: Uri): Lyrics? {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open lyrics")
        val bytes = stream.use { it.readUpTo(1_048_577) }
        require(bytes.size <= 1_048_576) { "Lyrics exceed 1 MB" }
        val charset = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> Charsets.UTF_16
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> Charsets.UTF_16
            else -> Charsets.UTF_8
        }
        return LyricsParser.parse(String(bytes, charset))
    }
}
