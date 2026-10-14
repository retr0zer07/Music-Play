package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.LyricsParser
import com.spheremusic.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerLyricsScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lyricsAreBelowControlsAndTimedUpdatesDoNotPullListenerOffCover() {
        val song = Song(46, Uri.EMPTY, "Original scroll test", "Sphere", "", 1, 120_000, null)
        val state = mutableStateOf(MusicState(
            current = song, durationMs = 120_000,
            lyrics = LyricsParser.parse("[00:00.00]Original first line\n[00:30.00]Original second line"),
        ))
        var seek = -1L
        compose.setContent {
            SphereTheme {
                PlayerScreen(state.value, SnackbarHostState(), {}, {}, {}, {}, {}, { seek = it }, {}, {}, {}, {})
            }
        }
        compose.runOnIdle { state.value = state.value.copy(positionMs = 30_000) }
        compose.onNodeWithContentDescription("Disco de ${song.title}").assertIsDisplayed()
        compose.onNodeWithText("Desliza para ver letras").performScrollTo().performClick()
        compose.onNodeWithText("Original second line").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(30_000L, seek) }
        compose.onNodeWithText("Portada").assertDoesNotExist()
    }
}
