package com.spheremusic.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import com.spheremusic.app.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class MusicRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("sphere_music", Context.MODE_PRIVATE)
    private val lyricsLock = Mutex()
    private var lyricsIndex: Map<String, List<Uri>>? = null

    fun songOrder(): SongOrder = SongOrder.entries.find {
        it.name == preferences.getString("song_order", SongOrder.RECENT.name)
    } ?: SongOrder.RECENT

    fun saveSongOrder(order: SongOrder) {
        preferences.edit().putString("song_order", order.name).apply()
    }

    fun lyricsFolder(): String? = preferences.getString("lyrics_folder", null)

    suspend fun setLyricsFolder(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        lyricsLock.withLock {
            val index = indexLyrics(uri)
            preferences.edit().putString("lyrics_folder", uri.toString()).apply()
            lyricsIndex = index
        }
    }

    suspend fun invalidateLyricsIndex() = lyricsLock.withLock { lyricsIndex = null }

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
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
        )
        val result = mutableListOf<Song>()
        val cursor = context.contentResolver.query(
            collection, columns, null, null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media._ID} DESC",
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
                    it.getString(6).orEmpty(), it.getLong(7),
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
            val automatic = lyricsLock.withLock {
                val folder = lyricsFolder()
                if (folder == null) null else {
                    val index = lyricsIndex ?: indexLyrics(Uri.parse(folder)).also { lyricsIndex = it }
                    LyricsMatcher.match(song.fileName, song.title, index)
                }
            }
            if (automatic != null) return@withContext readExternal(automatic)
            val stream = context.contentResolver.openInputStream(song.uri)
                ?: throw IOException("Cannot open audio")
            stream.use { EmbeddedLyrics.read(it)?.let(LyricsParser::parse) }
        }
    }

    private suspend fun indexLyrics(tree: Uri): Map<String, List<Uri>> {
        val documents = ArrayDeque<String>()
        documents.add(DocumentsContract.getTreeDocumentId(tree))
        val visited = mutableSetOf<String>()
        val index = mutableMapOf<String, MutableList<Uri>>()
        while (documents.isNotEmpty()) {
            coroutineContext.ensureActive()
            val parent = documents.removeFirst()
            if (!visited.add(parent)) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            val columns = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            val cursor = context.contentResolver.query(children, columns, null, null, null)
                ?: throw IOException("Cannot list lyrics folder")
            cursor.use {
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    val id = it.getString(0)
                    val name = it.getString(1).orEmpty()
                    if (it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        documents.add(id)
                    } else if (name.endsWith(".lrc", ignoreCase = true)) {
                        index.getOrPut(LyricsMatcher.key(name)) { mutableListOf() }
                            .add(DocumentsContract.buildDocumentUriUsingTree(tree, id))
                    }
                }
            }
        }
        return index
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
