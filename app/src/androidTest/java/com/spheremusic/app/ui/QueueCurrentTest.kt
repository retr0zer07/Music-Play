package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Song
import org.junit.Rule
import org.junit.Test

class QueueCurrentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun queueOpensAtCurrentTrackInsteadOfFirstTrack() {
        val songs = (1L..100L).map { Song(it, Uri.EMPTY, "Original $it", "Sphere", "", 1, 0, null) }
        compose.setContent {
            SphereTheme {
                QueuePanel(MusicState(queue = songs, current = songs[79]),
                    Modifier.height(400.dp), {}, { _, _ -> }, {})
            }
        }
        compose.onNodeWithContentDescription("Arrastrar Original 80 para cambiar su orden").assertIsDisplayed()
        compose.onNodeWithContentDescription("Arrastrar Original 1 para cambiar su orden").assertDoesNotExist()
    }
}
