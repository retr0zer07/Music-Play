package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.spheremusic.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class PlaybackVisualsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun discAndSelectedRowMoveWhilePlayingFreezeWhenPausedAndResumeFromSamePhase() {
        val song = Song(44, Uri.EMPTY, "Original visual test", "Sphere", "", 1, 0, null)
        val playing = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SphereTheme {
                Column {
                    RecordArtwork(song, playing.value, Modifier.size(220.dp))
                    SongRow(song, false, true, {}, {}, playing = playing.value)
                }
            }
        }
        fun recordPhase() = compose.onNodeWithContentDescription("Disco de ${song.title}")
            .fetchSemanticsNode().config[PlaybackPhaseKey]
        fun rowPhase() = compose.onNodeWithContentDescription(
            if (playing.value) "Reproduciendo ${song.title}" else "En pausa: ${song.title}",
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[PlaybackPhaseKey]
        compose.mainClock.advanceTimeBy(80)
        val firstDisc = recordPhase()
        val firstRow = rowPhase()
        compose.mainClock.advanceTimeBy(400)
        assertNotEquals(firstDisc, recordPhase())
        assertNotEquals(firstRow, rowPhase())
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeBy(80)
        val pausedDisc = recordPhase()
        val pausedRow = rowPhase()
        compose.mainClock.advanceTimeBy(400)
        assertEquals(pausedDisc, recordPhase(), 0f)
        assertEquals(pausedRow, rowPhase(), 0f)
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(240)
        assertNotEquals(pausedDisc, recordPhase())
        assertNotEquals(pausedRow, rowPhase())
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeBy(80)
    }

    @Test fun unselectedRowsDoNotShowPlaybackIndicator() {
        val song = Song(45, Uri.EMPTY, "Other original test", "Sphere", "", 1, 0, null)
        compose.setContent { SphereTheme { SongRow(song, false, false, {}, {}, playing = true) } }
        compose.onNodeWithContentDescription("Reproduciendo ${song.title}", useUnmergedTree = true).assertDoesNotExist()
    }
}
