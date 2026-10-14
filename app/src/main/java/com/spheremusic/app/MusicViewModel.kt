package com.spheremusic.app

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ComponentName
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.spheremusic.app.data.Lyrics
import com.spheremusic.app.data.MusicRepository
import com.spheremusic.app.data.Song
import com.spheremusic.app.data.SongOrder
import com.spheremusic.app.data.Playlist
import com.spheremusic.app.data.PlaylistStore
import com.spheremusic.app.data.AmbiguousLyricsException
import com.spheremusic.app.playback.PlaybackService
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicState(
    val songs: List<Song> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val loading: Boolean = false,
    val ready: Boolean = false,
    val current: Song? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val lyrics: Lyrics? = null,
    val lyricsLoading: Boolean = false,
    val queue: List<Song> = emptyList(),
    val songOrder: SongOrder = SongOrder.RECENT,
    val lyricsFolder: String? = null,
    val playlists: List<Playlist> = emptyList(),
)

class MusicViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val playlistStore = PlaylistStore(application.getSharedPreferences("sphere_playlists", Context.MODE_PRIVATE))
    private val playlistLock = Mutex()
    private val mutableState = MutableStateFlow(MusicState(
        favorites = repository.favorites(), songOrder = repository.songOrder(),
        lyricsFolder = repository.lyricsFolder(),
    ))
    val state = mutableState.asStateFlow()
    private val messages = Channel<Int>(Channel.BUFFERED)
    val notices = messages.receiveAsFlow()
    private val deletionRequests = Channel<IntentSender>(Channel.BUFFERED)
    val deletions = deletionRequests.receiveAsFlow()
    private var controller: MediaController? = null
    private var cleared = false
    private var scanJob: Job? = null
    private var lyricsJob: Job? = null
    private var lyricsSongId: Long? = null
    private val connection = MediaController.Builder(
        application, SessionToken(application, ComponentName(application, PlaybackService::class.java)),
    ).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            report(R.string.player_error, error)
        }
    }

    init {
        updatePlaylists { load() }
        connection.addListener({
            if (cleared) return@addListener
            try {
                controller = connection.get().also { it.addListener(listener) }
                mutableState.update { it.copy(ready = true) }
                syncPlayer()
            } catch (error: ExecutionException) {
                report(R.string.connection_error, error)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                report(R.string.connection_error, error)
            }
        }, ContextCompat.getMainExecutor(application))
        viewModelScope.launch {
            while (true) {
                syncPlayer()
                delay(400)
            }
        }
    }

    fun refresh() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true) }
            try {
                repository.invalidateLyricsIndex()
                val songs = repository.songs()
                mutableState.update { it.copy(songs = songs) }
                lyricsSongId = null
                syncPlayer()
            } catch (error: SecurityException) {
                report(R.string.library_error, error)
            } catch (error: IOException) {
                report(R.string.library_error, error)
            } finally {
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    fun play(songs: List<Song>, selected: Song, shuffled: Boolean = false) {
        val player = controller ?: return
        val index = songs.indexOfFirst { it.id == selected.id }
        if (index == -1) return
        player.shuffleModeEnabled = false
        player.setMediaItems(songs.map(Song::toMediaItem), index, 0)
        player.shuffleModeEnabled = shuffled
        player.prepare()
        player.play()
        syncPlayer()
    }

    fun togglePlayback() {
        controller?.let {
            if (it.isPlaying) it.pause() else {
                if (it.playbackState == Player.STATE_ENDED) it.seekTo(0)
                if (it.playbackState == Player.STATE_IDLE) it.prepare()
                it.play()
            }
        }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() {
        controller?.let { if (it.currentPosition > 3_000) it.seekTo(0) else it.seekToPreviousMediaItem() }
    }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceIn(0, state.value.durationMs)) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun playQueue(index: Int) {
        controller?.let { it.seekToDefaultPosition(index); it.play() }
    }

    fun moveQueue(songId: Long, targetId: Long) {
        val player = controller ?: return
        val from = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == songId.toString() }
            ?: return
        val to = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == targetId.toString() }
            ?: return
        if (from == to) return
        if (player.shuffleModeEnabled) {
            player.shuffleModeEnabled = false
            viewModelScope.launch { messages.send(R.string.queue_manual_order) }
        }
        player.moveMediaItem(from, to)
        syncPlayer()
    }

    fun setSongOrder(order: SongOrder) {
        repository.saveSongOrder(order)
        mutableState.update { it.copy(songOrder = order) }
    }

    fun setLyricsFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.setLyricsFolder(uri)
                mutableState.update { it.copy(lyricsFolder = repository.lyricsFolder()) }
                lyricsSongId = null
                syncPlayer()
                messages.send(R.string.lyrics_folder_saved)
            } catch (error: IOException) {
                report(R.string.lyrics_folder_error, error)
            } catch (error: SecurityException) {
                report(R.string.lyrics_folder_error, error)
            } catch (error: IllegalArgumentException) {
                report(R.string.lyrics_folder_error, error)
            }
        }
    }

    fun toggleFavorite(song: Song) {
        val favorites = state.value.favorites.toMutableSet()
        if (!favorites.add(song.id)) favorites.remove(song.id)
        repository.saveFavorites(favorites)
        mutableState.update { it.copy(favorites = favorites) }
    }

    fun createPlaylist(name: String, songId: Long?) = updatePlaylists { create(name, songId) }
    fun renamePlaylist(id: String, name: String) = updatePlaylists { rename(id, name) }
    fun deletePlaylist(id: String) = updatePlaylists { delete(id) }
    fun addToPlaylist(id: String, songId: Long) = updatePlaylists { addSong(id, songId) }
    fun removeFromPlaylist(id: String, songId: Long) = updatePlaylists { removeSong(id, songId) }

    private fun updatePlaylists(operation: PlaylistStore.() -> List<Playlist>) {
        viewModelScope.launch {
            try {
                playlistLock.withLock {
                    val playlists = withContext(Dispatchers.IO) { playlistStore.operation() }
                    mutableState.update { it.copy(playlists = playlists) }
                }
            } catch (error: IllegalArgumentException) {
                report(R.string.playlist_invalid_name, error)
            } catch (error: org.json.JSONException) {
                report(R.string.playlist_error, error)
            } catch (error: IOException) {
                report(R.string.playlist_error, error)
            }
        }
    }

    fun deletionError(error: Exception) = report(R.string.delete_error, error)

    fun requestDeletion(song: Song) {
        if (savedState.get<Long>("deleting_id") != null) {
            deletionError(IllegalStateException("A deletion is already pending"))
            return
        }
        viewModelScope.launch {
            val uri = if (Build.VERSION.SDK_INT >= 29 && song.volumeName.isNotBlank()) {
                ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(song.volumeName), song.id)
            } else song.uri
            savedState["deleting_id"] = song.id
            savedState["deleting_uri"] = uri.toString()
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    val sender = MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, listOf(uri))
                    deletionRequests.send(sender.intentSender)
                } else {
                    try {
                        repository.deleteAudio(uri)
                        finishDeletion(song.id)
                    } catch (error: SecurityException) {
                        if (Build.VERSION.SDK_INT >= 29 && error is RecoverableSecurityException) {
                            deletionRequests.send(error.userAction.actionIntent.intentSender)
                        } else throw error
                    }
                }
            } catch (error: SecurityException) {
                deletionError(error)
                clearDeletion()
            } catch (error: IOException) {
                deletionError(error)
                clearDeletion()
            } catch (error: IllegalArgumentException) {
                deletionError(error)
                clearDeletion()
            }
        }
    }

    fun completeDeletion(approved: Boolean) {
        if (!approved) { clearDeletion(); return }
        val id = savedState.get<Long>("deleting_id") ?: return
        val uri = savedState.get<String>("deleting_uri")?.let(Uri::parse) ?: return
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT < 30) repository.deleteAudio(uri)
                if (repository.audioExists(uri)) throw IOException("Android did not delete the audio")
                finishDeletion(id)
            } catch (error: SecurityException) {
                deletionError(error)
            } catch (error: IOException) {
                deletionError(error)
            } finally {
                clearDeletion()
            }
        }
    }

    private suspend fun finishDeletion(id: Long) {
        controller?.let { player ->
            for (index in player.mediaItemCount - 1 downTo 0) {
                if (player.getMediaItemAt(index).mediaId == id.toString()) player.removeMediaItem(index)
            }
        }
        repository.forgetSong(id)
        mutableState.update { it.copy(songs = it.songs.filterNot { song -> song.id == id }, favorites = it.favorites - id) }
        syncPlayer()
        try {
            playlistLock.withLock {
                val playlists = withContext(Dispatchers.IO) { playlistStore.removeSongEverywhere(id) }
                mutableState.update { it.copy(playlists = playlists) }
            }
        } catch (error: IOException) {
            report(R.string.playlist_error, error)
        } catch (error: org.json.JSONException) {
            report(R.string.playlist_error, error)
        }
        clearDeletion()
        messages.send(R.string.song_deleted)
    }

    private fun clearDeletion() {
        savedState.remove<Long>("deleting_id")
        savedState.remove<String>("deleting_uri")
    }

    fun linkLyrics(song: Song, uri: Uri) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            mutableState.update { it.copy(lyricsLoading = true) }
            try {
                val lyrics = repository.linkLyrics(song, uri)
                if (state.value.current?.id == song.id) mutableState.update { it.copy(lyrics = lyrics) }
                messages.send(R.string.lyrics_linked)
            } catch (error: IllegalArgumentException) {
                report(R.string.invalid_lyrics, error)
            } catch (error: IOException) {
                report(R.string.lyrics_error, error)
            } catch (error: SecurityException) {
                report(R.string.lyrics_error, error)
            } finally {
                mutableState.update { it.copy(lyricsLoading = false) }
            }
        }
    }

    private fun syncPlayer() {
        val player = controller ?: return
        val songsById = state.value.songs.associateBy { it.id.toString() }
        val current = songsById[player.currentMediaItem?.mediaId]
        val duration = player.duration.takeUnless { it == C.TIME_UNSET || it < 0 } ?: current?.durationMs ?: 0
        mutableState.update {
            it.copy(
                current = current, playing = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0), durationMs = duration,
                shuffle = player.shuffleModeEnabled, repeat = player.repeatMode,
                queue = (0 until player.mediaItemCount).mapNotNull { index ->
                    songsById[player.getMediaItemAt(index).mediaId]
                },
            )
        }
        if (current?.id != lyricsSongId) {
            lyricsSongId = current?.id
            lyricsJob?.cancel()
            mutableState.update { it.copy(lyrics = null, lyricsLoading = current != null) }
            if (current != null) {
                lyricsJob = viewModelScope.launch {
                    try {
                        val lyrics = repository.lyrics(current)
                        mutableState.update { it.copy(lyrics = lyrics) }
                    } catch (error: AmbiguousLyricsException) {
                        report(R.string.lyrics_ambiguous, error)
                    } catch (error: IOException) {
                        report(R.string.lyrics_error, error)
                    } catch (error: SecurityException) {
                        report(R.string.lyrics_error, error)
                    } catch (error: IllegalArgumentException) {
                        report(R.string.invalid_lyrics, error)
                    } finally {
                        mutableState.update { it.copy(lyricsLoading = false) }
                    }
                }
            }
        }
    }

    private fun report(message: Int, error: Exception) {
        Log.e("SphereMusic", getApplication<Application>().getString(message), error)
        viewModelScope.launch { messages.send(message) }
    }

    override fun onCleared() {
        cleared = true
        controller?.removeListener(listener)
        MediaController.releaseFuture(connection)
        super.onCleared()
    }
}
