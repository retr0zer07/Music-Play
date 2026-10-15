package com.spheremusic.app.ui

import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Song
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun playerControlsFollowRecordAndQueueReturnsWithoutLosingLyricsScroll() {
        val song = Song(1, Uri.EMPTY, "Canción de prueba", "Sphere", "Prueba", 1, 120_000, null)
        val background = Color(0xFF29203C)
        compose.setContent {
            SphereTheme {
                CompositionLocalProvider(LocalArtworkBackground provides background) {
                PlayerScreen(
                    MusicState(current = song, queue = listOf(song), durationMs = 120_000),
                    SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                )
                }
            }
        }
        compose.onNodeWithText(song.title).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(song.title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Song title must contrast with adaptive backgrounds",
            ColorUtils.calculateContrast(layouts.single().layoutInput.style.color.toArgb(), background.toArgb()) >= 4.5)
        compose.onNodeWithText("Portada").assertDoesNotExist()
        compose.onNodeWithText("Cola").assertDoesNotExist()
        val record = compose.onNodeWithContentDescription("Disco de ${song.title}").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText(song.title).fetchSemanticsNode().boundsInRoot
        assertTrue("Song title must follow the disc", title.top >= record.bottom)
        compose.onNodeWithContentDescription("Reproducir").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Siguiente canción").assertIsDisplayed()
        compose.onNodeWithContentDescription("Canción anterior").assertIsDisplayed()
        val seek = compose.onNodeWithContentDescription("Posición de reproducción").fetchSemanticsNode().boundsInRoot
        val play = compose.onNodeWithContentDescription("Reproducir").fetchSemanticsNode().boundsInRoot
        assertTrue("Playback controls must follow seek bar", play.top >= seek.bottom)
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "visual-checks")
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "player.png").outputStream().use {
            check(compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        compose.onNodeWithContentDescription("Abrir cola de reproducción").performScrollTo().performClick()
        compose.onNodeWithText("Cola de reproducción").assertIsDisplayed()
        compose.onNodeWithContentDescription("Arrastrar ${song.title} para cambiar su orden").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver al reproductor").performClick()
        compose.onNodeWithContentDescription("Disco de ${song.title}").assertIsDisplayed()
    }
}
