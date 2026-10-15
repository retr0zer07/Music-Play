package com.spheremusic.app.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Song
import com.spheremusic.app.data.SongOrder
import java.io.File
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectionActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectionKeepsCoversSharesAndQueuesInListOrderWithoutPlaying() {
        val songs = listOf("Beta", "Alpha").mapIndexed { index, title ->
            Song(index.toLong(), Uri.EMPTY, title, "Sphere", title, index.toLong(), 120_000, null)
        }
        var played = false
        var shared = emptyList<Song>()
        var queued = emptyList<Song>()
        var playlistIds = emptyList<Long>()
        compose.setContent {
            SphereTheme {
                SphereApp(
                    state = MusicState(songs = songs, songOrder = SongOrder.TITLE, ready = true),
                    notices = emptyFlow(), permission = true, denied = false,
                    onPermission = {}, onSettings = {}, onRefresh = {}, onPlay = { _, _, _ -> played = true },
                    onFavorite = {}, onToggle = {}, onNext = {}, onPrevious = {}, onSeek = {},
                    onShuffle = {}, onRepeat = {}, onQueuePlay = {}, onLinkLyrics = {},
                    onQueueMove = { _, _ -> }, onSongOrder = {}, onLyricsFolder = {},
                    onCreatePlaylist = { _, _ -> }, onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                    onAddToPlaylist = { _, _ -> }, onRemoveFromPlaylist = { _, _ -> }, onDeleteSong = {},
                    onCreateManyPlaylist = { _, ids -> playlistIds = ids },
                    onPlayNextSongs = { queued = it }, onShareSongs = { shared = it },
                )
            }
        }
        fun selectBoth() {
            compose.onNodeWithText("Beta").performTouchInput { longClick() }
            compose.onNodeWithText("Alpha").performClick()
        }
        selectBoth()
        songs.forEach {
            compose.onNodeWithContentDescription("Portada de ${it.album}", useUnmergedTree = true).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("Compartir canción").performClick()
        compose.runOnIdle {
            assertEquals(listOf(songs[1], songs[0]), shared)
            assertEquals(false, played)
        }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "visual-checks")
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "selection.png").outputStream().use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        compose.onNodeWithContentDescription("Reproducir a continuación").performClick()
        compose.runOnIdle { assertEquals(listOf(songs[1], songs[0]), queued) }
        selectBoth()
        compose.onNodeWithContentDescription("Agregar a playlist").performClick()
        compose.onNodeWithText("Crear playlist").performClick()
        compose.onNodeWithText("Nombre de la playlist").performTextInput("Selected")
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertEquals(listOf(songs[1].id, songs[0].id), playlistIds) }
    }

    @Test fun currentSongActionsDoNotOfferPlayingItNext() {
        val song = Song(3, Uri.EMPTY, "Current", "Sphere", "", 3, 120_000, null)
        compose.setContent {
            SphereTheme {
                SongActionsDialog(song, false, {}, {}, {}, {}, {}, {}, canPlayNext = false)
            }
        }
        compose.onNodeWithText("Reproducir a continuación").assertDoesNotExist()
        compose.onNodeWithText("Agregar a playlist").assertIsDisplayed()
        compose.onNodeWithText("Compartir canción").assertIsDisplayed()
    }
}
