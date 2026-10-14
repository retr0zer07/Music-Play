package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Playlist
import com.spheremusic.app.data.Song
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun createsPlaylistAddsSongAndCancelsDestructiveAction() {
        val song = Song(71, Uri.EMPTY, "Original track", "Sphere", "", 1, 120_000, null)
        val state = mutableStateOf(MusicState(songs = listOf(song), ready = true))
        var deleted = false
        var played = emptyList<Song>()
        compose.setContent {
            SphereTheme {
                SphereApp(
                    state = state.value, notices = emptyFlow(), permission = true, denied = false,
                    onPermission = {}, onSettings = {}, onRefresh = {}, onPlay = { songs, _, _ -> played = songs },
                    onFavorite = {}, onToggle = {}, onNext = {}, onPrevious = {}, onSeek = {},
                    onShuffle = {}, onRepeat = {}, onQueuePlay = {}, onLinkLyrics = {},
                    onQueueMove = { _, _ -> }, onSongOrder = {}, onLyricsFolder = {},
                    onCreatePlaylist = { name, id ->
                        state.value = state.value.copy(playlists = listOf(Playlist("test", name, listOfNotNull(id))))
                    },
                    onRenamePlaylist = { _, _ -> }, onDeletePlaylist = {},
                    onAddToPlaylist = { _, id ->
                        state.value = state.value.copy(playlists = state.value.playlists.map { it.copy(songIds = listOf(id)) })
                    },
                    onRemoveFromPlaylist = { _, _ -> }, onDeleteSong = { deleted = true },
                )
            }
        }
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText("Crear playlist").performClick()
        compose.onNodeWithText("Nombre de la playlist").performTextInput("My playlist")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("My playlist").assertExists()
        compose.onNodeWithText("Biblioteca").performClick()
        compose.onNodeWithContentDescription("Opciones de Original track").performClick()
        compose.onNodeWithText("Agregar a playlist").performClick()
        compose.onNodeWithText("My playlist").performClick()
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText("My playlist").performClick()
        compose.onNodeWithText("Original track").performClick()
        compose.runOnIdle { assertEquals(listOf(song), played) }
        compose.onNodeWithContentDescription("Opciones de Original track").performClick()
        compose.onNodeWithText("Quitar de esta playlist").assertExists()
        compose.onNodeWithText("Borrar del teléfono").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(false, deleted) }
        compose.onAllNodesWithText("Original track")[0].assertExists()
    }
}
