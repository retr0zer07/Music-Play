package com.spheremusic.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.os.Build
import com.spheremusic.app.R
import java.io.IOException
import java.io.File
import java.util.UUID
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

    fun forgetSong(id: Long) {
        val linked = preferences.getString("lyrics_$id", null)
        saveFavorites(favorites() - id)
        preferences.edit().remove("lyrics_$id").apply()
        linked?.let { deletePrivateLyrics(Uri.parse(it)) }
    }

    suspend fun saveCopiedLyrics(songId: Long, text: String): Lyrics = withContext(Dispatchers.IO) {
        val lyrics = CopiedLyrics.validate(text)
        val directory = File(context.filesDir, "lyrics")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create lyrics directory")
        val file = File(directory, "$songId-${UUID.randomUUID()}.txt")
        val previous = preferences.getString("lyrics_$songId", null)
        try {
            file.outputStream().use { it.write(text.toByteArray(Charsets.UTF_8)) }
            if (!preferences.edit().putString("lyrics_$songId", Uri.fromFile(file).toString()).commit()) {
                throw IOException("Cannot link copied lyrics")
            }
        } catch (error: IOException) {
            if (file.exists() && !file.delete()) android.util.Log.w("SphereMusic", "Cannot clean incomplete lyrics file")
            throw error
        }
        previous?.let { deletePrivateLyrics(Uri.parse(it)) }
        lyrics
    }

    private fun deletePrivateLyrics(uri: Uri) {
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        val file = File(path)
        if (file.parentFile != File(context.filesDir, "lyrics")) return
        if (file.exists() && !file.delete()) android.util.Log.w("SphereMusic", "Cannot remove previous private lyrics")
    }

    suspend fun audioExists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)
            ?: throw IOException("Cannot verify audio deletion")
        cursor.use { it.moveToFirst() }
    }

    suspend fun deleteAudio(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.delete(uri, null, null)
        if (audioExists(uri)) throw IOException("Audio was not deleted")
    }

    suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = mutableListOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
        )
        if (Build.VERSION.SDK_INT >= 29) {
            columns += MediaStore.Audio.Media.RELATIVE_PATH
            columns += MediaStore.Audio.Media.VOLUME_NAME
        }
        val result = mutableListOf<Song>()
        val cursor = context.contentResolver.query(
            collection, columns.toTypedArray(), null, null,
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
                    if (Build.VERSION.SDK_INT >= 29) it.getString(8).orEmpty() else "",
                    if (Build.VERSION.SDK_INT >= 29) it.getString(9).orEmpty() else "",
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
                    val tree = Uri.parse(folder)
                    val siblingDirectory = LyricsLocation.siblingDirectory(
                        tree.authority.orEmpty(), DocumentsContract.getTreeDocumentId(tree),
                        song.volumeName, song.relativePath,
                    )
                    val sibling = siblingDirectory?.let {
                        // Query siblings every time so new sidecars are visible without rescanning all folders.
                        LyricsMatcher.match(song.fileName, song.title, directoryLyrics(tree, it))
                    }
                    sibling ?: run {
                        val index = lyricsIndex ?: indexLyrics(tree).also { lyricsIndex = it }
                        LyricsMatcher.match(song.fileName, song.title, index) ?: run {
                            // A cached miss can mean a new download; refresh before declaring no match.
                            val refreshed = indexLyrics(tree).also { lyricsIndex = it }
                            LyricsMatcher.match(song.fileName, song.title, refreshed)
                        }
                    }
                }
            }
            if (automatic != null) return@withContext readExternal(automatic)
                ?: throw IllegalArgumentException("Matched LRC contains no lyrics")
            val stream = context.contentResolver.openInputStream(song.uri)
                ?: throw IOException("Cannot open audio")
            stream.use { EmbeddedLyrics.read(it)?.let(LyricsParser::parse) }
        }
    }

    private suspend fun directoryLyrics(tree: Uri, directoryId: String): Map<String, List<Uri>> {
        val result = mutableMapOf<String, MutableList<Uri>>()
        for (document in directoryDocuments(tree, directoryId)) {
            if (document.mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                document.name.endsWith(".lrc", ignoreCase = true)
            ) {
                result.getOrPut(LyricsMatcher.key(document.name)) { mutableListOf() }
                    .add(DocumentsContract.buildDocumentUriUsingTree(tree, document.id))
            }
        }
        return result
    }

    private data class Document(val id: String, val name: String, val mimeType: String)

    private suspend fun directoryDocuments(tree: Uri, directoryId: String): List<Document> {
        coroutineContext.ensureActive()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directoryId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val cursor = context.contentResolver.query(children, columns, null, null, null)
            ?: throw IOException("Cannot list lyrics folder")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    add(Document(it.getString(0), it.getString(1).orEmpty(), it.getString(2).orEmpty()))
                }
            }
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
            for (document in directoryDocuments(tree, parent)) {
                if (document.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    documents.add(document.id)
                } else if (document.name.endsWith(".lrc", ignoreCase = true)) {
                    index.getOrPut(LyricsMatcher.key(document.name)) { mutableListOf() }
                        .add(DocumentsContract.buildDocumentUriUsingTree(tree, document.id))
                }
            }
        }
        return index
    }

    suspend fun linkLyrics(song: Song, uri: Uri): Lyrics = withContext(Dispatchers.IO) {
        val lyrics = readExternal(uri) ?: throw IllegalArgumentException("Empty lyrics")
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val previous = preferences.getString("lyrics_${song.id}", null)
        preferences.edit().putString("lyrics_${song.id}", uri.toString()).apply()
        previous?.let { if (it != uri.toString()) deletePrivateLyrics(Uri.parse(it)) }
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
