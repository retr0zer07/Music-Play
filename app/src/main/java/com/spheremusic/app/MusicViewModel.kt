package com.spheremusic.app

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.spheremusic.app.data.Lyrics
import com.spheremusic.app.data.MusicRepository
import com.spheremusic.app.data.Song
import com.spheremusic.app.playback.PlaybackService
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Job
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
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val mutableState = MutableStateFlow(MusicState(favorites = repository.favorites()))
    val state = mutableState.asStateFlow()
    private val messages = Channel<Int>(Channel.BUFFERED)
    val notices = messages.receiveAsFlow()
    private var controller: MediaController? = null
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
        connection.addListener({
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
                val songs = repository.songs()
                mutableState.update { it.copy(songs = songs) }
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
        player.shuffleModeEnabled = shuffled
        player.setMediaItems(songs.map(Song::toMediaItem), index, 0)
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

    fun toggleFavorite(song: Song) {
        val favorites = state.value.favorites.toMutableSet()
        if (!favorites.add(song.id)) favorites.remove(song.id)
        repository.saveFavorites(favorites)
        mutableState.update { it.copy(favorites = favorites) }
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
        controller?.removeListener(listener)
        MediaController.releaseFuture(connection)
        super.onCleared()
    }
}
