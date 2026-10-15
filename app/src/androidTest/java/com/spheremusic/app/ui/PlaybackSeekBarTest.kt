package com.spheremusic.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackSeekBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun largeTouchTargetDragsWithoutSeekingUntilReleaseAndDisablesWithoutDuration() {
        val progress = mutableStateOf(0f)
        val enabled = mutableStateOf(true)
        var committed = -1f
        var commits = 0
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            SphereTheme {
                PlaybackSeekBar(
                    progress.value, { progress.value = it }, { committed = progress.value; commits++ },
                    0f..100f, enabled.value,
                    Modifier.semantics { contentDescription = "Test seek" }, playing = false,
                )
            }
        }
        val bar = compose.onNodeWithContentDescription("Test seek")
        assertTrue(bar.fetchSemanticsNode().boundsInRoot.height / density >= 48f)
        bar.performTouchInput {
            down(centerLeft.copy(x = width * 0.2f))
            moveTo(center.copy(x = width * 0.6f))
        }
        compose.runOnIdle { assertEquals(0, commits) }
        bar.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(1, commits)
            assertTrue(committed in 50f..75f)
            enabled.value = false
        }
        bar.assertIsNotEnabled()
        bar.performTouchInput { swipe(centerLeft, centerRight, 200) }
        compose.runOnIdle { assertEquals(1, commits) }
    }
}
