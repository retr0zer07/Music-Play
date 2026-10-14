package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.LyricsParser
import com.spheremusic.app.data.Song
import org.junit.Rule
import org.junit.Test

class LyricsActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadedLyricsHideSetupButtonsButMissingLyricsOfferThem() {
        val song = Song(1, Uri.EMPTY, "Original test", "Sphere", "", 1, 120_000, null)
        val state = mutableStateOf(MusicState(current = song, lyrics = LyricsParser.parse("Original line")))
        compose.setContent {
            SphereTheme {
                PlayerScreen(state.value, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Letras").performClick()
        compose.onNodeWithText("Original line").assertExists()
        compose.onNodeWithText("Vincular letras (.lrc / .txt)").assertDoesNotExist()
        compose.onNodeWithText("Carpeta de letras automáticas").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(lyrics = null) }
        compose.onNodeWithText("Vincular letras (.lrc / .txt)").assertExists()
        compose.onNodeWithText("Carpeta de letras automáticas").assertExists()
        compose.runOnIdle { state.value = state.value.copy(lyricsLoading = true) }
        compose.onNodeWithText("Vincular letras (.lrc / .txt)").assertDoesNotExist()
    }
}
