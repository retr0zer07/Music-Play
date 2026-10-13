package com.spheremusic.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.spheremusic.app.MusicState
import com.spheremusic.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QueueDragTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingHandleChangesQueueOrder() {
        val songs = (1L..3L).map { id -> Song(id, Uri.EMPTY, "Test $id", "Sphere", "", 1, 0, null) }
        val queue = mutableStateOf(songs)
        compose.setContent {
            SphereTheme {
                QueuePanel(
                    MusicState(queue = queue.value, current = songs.first()), androidx.compose.ui.Modifier.height(400.dp),
                    {}, { from, target ->
                        val reordered = queue.value.toMutableList()
                        val source = reordered.indexOfFirst { it.id == from }
                        val destination = reordered.indexOfFirst { it.id == target }
                        reordered.add(destination, reordered.removeAt(source))
                        queue.value = reordered
                    }, {},
                )
            }
        }
        val first = compose.onNodeWithContentDescription("Arrastrar Test 1 para cambiar su orden")
        val second = compose.onNodeWithContentDescription("Arrastrar Test 2 para cambiar su orden")
        first.assertIsDisplayed()
        val distance = second.fetchSemanticsNode().boundsInRoot.center.y -
            first.fetchSemanticsNode().boundsInRoot.center.y
        first.performTouchInput { swipe(center, center + Offset(0f, distance), 800) }
        compose.runOnIdle { assertEquals(listOf(2L, 1L, 3L), queue.value.map { it.id }) }
    }
}
