package com.spheremusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
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

class MainActivity : ComponentActivity() {
    private val model: MusicViewModel by viewModels()

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
            SphereTheme {
                val state by model.state.collectAsStateWithLifecycle()
                var permission by remember { mutableStateOf(hasPermission(audioPermission)) }
                var denied by rememberSaveable { mutableStateOf(false) }
                var pendingLyricsSongId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedLyricsUri by rememberSaveable { mutableStateOf<String?>(null) }
                val requestPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> permission = granted; denied = !granted }
                val selectLyrics = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> selectedLyricsUri = uri?.toString() }

                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) permission = hasPermission(audioPermission)
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
                    onLinkLyrics = { song ->
                        pendingLyricsSongId = song.id
                        selectLyrics.launch(arrayOf("*/*"))
                    },
                )
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.hideStatusBar()
    }
}
