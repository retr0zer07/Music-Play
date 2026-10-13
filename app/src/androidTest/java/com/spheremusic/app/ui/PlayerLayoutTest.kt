package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Song
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun playerControlsFollowArtworkWithoutOverlappingTabs() {
        val song = Song(1, Uri.EMPTY, "Canción de prueba", "Sphere", "Prueba", 1, 120_000, null)
        compose.setContent {
            SphereTheme {
                PlayerScreen(
                    MusicState(current = song, queue = listOf(song), durationMs = 120_000),
                    SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        }
        compose.onNodeWithText(song.title).performScrollTo().assertIsDisplayed()
        val tabs = compose.onNodeWithText("Cola").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(song.title).fetchSemanticsNode().boundsInRoot
        assertTrue("Song title must follow tabs, not overlap them", title.top >= tabs.bottom)
        compose.onNodeWithContentDescription("Reproducir").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente canción").assertIsDisplayed()
        compose.onNodeWithContentDescription("Canción anterior").assertIsDisplayed()
        val seek = compose.onNodeWithContentDescription("Posición de reproducción").fetchSemanticsNode().boundsInRoot
        val play = compose.onNodeWithContentDescription("Reproducir").fetchSemanticsNode().boundsInRoot
        assertTrue("Playback controls must follow seek bar", play.top >= seek.bottom)
    }
}
