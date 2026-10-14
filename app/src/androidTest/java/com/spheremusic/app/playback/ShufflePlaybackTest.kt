package com.spheremusic.app.playback

import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShufflePlaybackTest {
    @Test fun controllerShuffleChangesNextTrackWithoutRestartingAndVisitsWholeQueue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val future = MediaController.Builder(context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        val controller = future.get(10, TimeUnit.SECONDS)
        fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (System.nanoTime() < deadline) {
                var done = false
                onMain { done = condition() }
                if (done) return
                Thread.sleep(50)
            }
            throw AssertionError("Playback command did not reach service")
        }
        try {
            onMain {
                controller.shuffleModeEnabled = false
                controller.setMediaItems((0..4).map {
                    MediaItem.Builder().setMediaId("shuffle-test-$it")
                        .setUri("content://com.spheremusic.test/not-played/$it").build()
                }, 1, 12_000)
                controller.repeatMode = Player.REPEAT_MODE_ONE
                controller.shuffleModeEnabled = true
            }
            await { controller.shuffleModeEnabled && controller.repeatMode == Player.REPEAT_MODE_OFF }
            onMain {
                assertEquals(1, controller.currentMediaItemIndex)
                assertEquals(12_000, controller.currentPosition)
                assertNotEquals(2, controller.nextMediaItemIndex)
                assertTrue(controller.nextMediaItemIndex >= 0)
            }
            val visited = mutableSetOf(1)
            repeat(4) {
                var next = -1
                onMain { next = controller.nextMediaItemIndex; controller.seekToNextMediaItem() }
                await { controller.currentMediaItemIndex == next }
                assertTrue("Each track must be visited once", visited.add(next))
            }
            assertEquals((0..4).toSet(), visited)
            onMain {
                assertEquals(-1, controller.nextMediaItemIndex)
                controller.shuffleModeEnabled = false
                controller.seekToDefaultPosition(1)
            }
            await { !controller.shuffleModeEnabled && controller.currentMediaItemIndex == 1 }
            onMain { assertEquals(2, controller.nextMediaItemIndex) }
        } finally {
            onMain { controller.clearMediaItems(); controller.repeatMode = Player.REPEAT_MODE_OFF; controller.release() }
        }
    }
}
