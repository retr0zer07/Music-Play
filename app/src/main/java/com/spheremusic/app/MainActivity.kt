package com.spheremusic.app

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ClipData
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spheremusic.app.ui.SphereApp
import com.spheremusic.app.ui.SphereTheme
import com.spheremusic.app.ui.hideStatusBar
import com.spheremusic.app.data.CopiedLyrics
import com.spheremusic.app.data.Song
import com.spheremusic.app.ui.LocalArtworkBackground
import com.spheremusic.app.ui.rememberArtworkBackground

class MainActivity : ComponentActivity() {
    private val model: MusicViewModel by viewModels()
    private val clipboardHandler = Handler(Looper.getMainLooper())
    private val importClipboard = Runnable {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && model.awaitingCopiedLyrics()) {
            model.receiveCopiedLyrics(readCopiedText())
        }
    }

    private fun readCopiedText(): String? {
        val clipboard = getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        // Never resolve clipboard URIs or request data from another app.
        val text = clip.getItemAt(0).text ?: return null
        if (text.length > CopiedLyrics.MAX_BYTES) {
            model.clipboardError(IllegalArgumentException("Copied text exceeds 1 MB"))
            return null
        }
        return text.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.hideStatusBar()
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            val artworkBackground = rememberArtworkBackground(state.current)
            CompositionLocalProvider(LocalArtworkBackground provides artworkBackground) {
            SphereTheme {
                var permission by remember { mutableStateOf(hasPermission(audioPermission)) }
                var denied by rememberSaveable { mutableStateOf(false) }
                var pendingLyricsSongId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedLyricsUri by rememberSaveable { mutableStateOf<String?>(null) }
                var legacyDeleteIds by rememberSaveable { mutableStateOf(longArrayOf()) }
                val requestPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> permission = granted; denied = !granted }
                val selectLyrics = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> selectedLyricsUri = uri?.toString() }
                val selectLyricsFolder = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri -> uri?.let(model::setLyricsFolder) }
                val deleteRequest = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult(),
                ) { result -> model.completeDeletion(result.resultCode == RESULT_OK) }
                val writePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    val songs = state.songs.filter { it.id in legacyDeleteIds }
                    legacyDeleteIds = longArrayOf()
                    if (granted && songs.isNotEmpty()) model.requestDeletions(songs)
                    else if (!granted) model.deletionError(SecurityException("Storage write permission denied"))
                }
                LaunchedEffect(model) {
                    model.deletions.collect { sender ->
                        try {
                            deleteRequest.launch(IntentSenderRequest.Builder(sender).build())
                        } catch (error: IntentSender.SendIntentException) {
                            model.completeDeletion(false)
                            model.deletionError(error)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) permission = hasPermission(audioPermission)
                        if (event == Lifecycle.Event.ON_PAUSE) {
                            model.lyricsSearchLeftApp()
                            clipboardHandler.removeCallbacks(importClipboard)
                        }
                        if (event == Lifecycle.Event.ON_RESUME) {
                            clipboardHandler.postDelayed(importClipboard, 600)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(permission) { if (permission) model.refresh() }
                LaunchedEffect(state.songs, selectedLyricsUri) {
                    val uri = selectedLyricsUri
                    val song = state.songs.find { it.id == pendingLyricsSongId }
                    if (uri != null && song != null) {
                        model.linkLyrics(song, Uri.parse(uri))
                        selectedLyricsUri = null
                        pendingLyricsSongId = null
                    }
                }
                SphereApp(
                    state = state,
                    notices = model.notices,
                    permission = permission,
                    denied = denied,
                    onPermission = { requestPermission.launch(audioPermission) },
                    onSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    },
                    onRefresh = model::refresh,
                    onPlay = model::play,
                    onFavorite = model::toggleFavorite,
                    onToggle = model::togglePlayback,
                    onNext = model::next,
                    onPrevious = model::previous,
                    onSeek = model::seek,
                    onShuffle = model::toggleShuffle,
                    onRepeat = model::cycleRepeat,
                    onQueuePlay = model::playQueue,
                    onQueueMove = model::moveQueue,
                    onSongOrder = model::setSongOrder,
                    onLyricsFolder = { selectLyricsFolder.launch(state.lyricsFolder?.let(Uri::parse)) },
                    onSearchLyrics = { song ->
                        val uri = Uri.Builder().scheme("https").authority("www.google.com").path("search")
                            .appendQueryParameter("q", CopiedLyrics.query(song)).build()
                        model.startLyricsSearch(song, readCopiedText())
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (error: ActivityNotFoundException) {
                            model.lyricsSearchError(error)
                        } catch (error: SecurityException) {
                            model.lyricsSearchError(error)
                        }
                    },
                    onPasteLyrics = ::readCopiedText,
                    onSaveCopiedLyrics = model::saveCopiedLyrics,
                    onCancelLyricsImport = model::cancelLyricsSearch,
                    onCreatePlaylist = model::createPlaylist,
                    onRenamePlaylist = model::renamePlaylist,
                    onDeletePlaylist = model::deletePlaylist,
                    onAddToPlaylist = model::addToPlaylist,
                    onAddManyToPlaylist = model::addManyToPlaylist,
                    onCreateManyPlaylist = model::createManyPlaylist,
                    onPlayNext = model::playNext,
                    onShare = { shareSongs(listOf(it)) },
                    onPlayNextSongs = model::playNextSongs,
                    onShareSongs = ::shareSongs,
                    onDeleteSongs = { songs ->
                        if (Build.VERSION.SDK_INT <= 28 && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            legacyDeleteIds = songs.map { it.id }.toLongArray()
                            writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else model.requestDeletions(songs)
                    },
                    onRemoveFromPlaylist = model::removeFromPlaylist,
                    onDeleteSong = { song ->
                        if (Build.VERSION.SDK_INT <= 28 && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            legacyDeleteIds = longArrayOf(song.id)
                            writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else model.requestDeletion(song)
                    },
                    onLinkLyrics = { song ->
                        pendingLyricsSongId = song.id
                        selectLyrics.launch(arrayOf("*/*"))
                    },
                )
            }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun shareSongs(songs: List<Song>) {
        try {
            startActivity(Intent.createChooser(audioShareIntent(songs), getString(R.string.share_song)))
        } catch (error: ActivityNotFoundException) {
            model.shareError(error)
        } catch (error: SecurityException) {
            model.shareError(error)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.hideStatusBar()
            clipboardHandler.removeCallbacks(importClipboard)
            clipboardHandler.postDelayed(importClipboard, 200)
        }

    }

    override fun onDestroy() {
        clipboardHandler.removeCallbacks(importClipboard)
        super.onDestroy()
    }
}

internal fun audioShareIntent(songs: List<Song>): Intent {
    require(songs.isNotEmpty()) { "Select audio to share" }
    val uris = songs.distinctBy { it.id }.map { it.uri }
    return Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "audio/*"
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
        else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        clipData = ClipData.newRawUri(songs.first().title, uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
