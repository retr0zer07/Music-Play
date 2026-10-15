package com.spheremusic.app.playback

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.rule.GrantPermissionRule
import com.spheremusic.app.MainActivity
import com.spheremusic.app.MusicViewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryPlaybackActionsTest {
    @get:Rule(order = 0) val permission = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun playNextPreservesPositionDeduplicatesAndEqualizerFollowsAudioSession() {
        compose.mainClock.autoAdvance = false
        val context = compose.activity.applicationContext
        val resolver = context.contentResolver
        val uris = mutableListOf<android.net.Uri>()
        lateinit var model: MusicViewModel
        try {
            repeat(3) { index ->
                val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "Sphere-Actions-${System.nanoTime()}-$index.wav")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SphereMusicTest")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                }))
                uris.add(uri)
                val samples = ByteArray(16_000 * 30)
                val wav = ByteBuffer.allocate(44 + samples.size).order(ByteOrder.LITTLE_ENDIAN)
                    .put("RIFF".toByteArray()).putInt(36 + samples.size)
                    .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                    .putInt(8_000).putInt(16_000).putShort(2).putShort(16)
                    .put("data".toByteArray()).putInt(samples.size).put(samples).array()
                checkNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
                if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }, null, null)
            }
            val ids = uris.map(ContentUris::parseId)
            compose.runOnIdle { model = ViewModelProvider(compose.activity)[MusicViewModel::class.java]; model.refresh() }
            compose.waitUntil(10_000) { model.state.value.ready && model.state.value.songs.count { it.id in ids } == 3 }
            val songs = ids.map { id -> model.state.value.songs.single { it.id == id } }
            compose.runOnIdle { model.play(songs, songs[0]) }
            compose.waitUntil(10_000) { model.state.value.playing }
            compose.waitUntil(10_000) { EqualizerControl.state.value.frequencies.isNotEmpty() || EqualizerControl.state.value.error }
            compose.runOnIdle { model.togglePlayback(); model.seek(5_000) }
            compose.waitUntil(5_000) { !model.state.value.playing && model.state.value.positionMs >= 5_000 }
            compose.runOnIdle { model.playNext(songs[2]) }
            compose.waitUntil(5_000) { model.state.value.queue.map { it.id } == listOf(ids[0], ids[2], ids[1]) }
            compose.runOnIdle {
                assertEquals(ids[0], model.state.value.current?.id)
                assertTrue(model.state.value.positionMs in 5_000..5_500)
                model.playNext(songs[2])
            }
            compose.waitUntil(5_000) { model.state.value.queue.size == 3 }
            assertEquals(1, model.state.value.queue.count { it.id == ids[2] })
            // Unsupported effects must be explicit, not a success-shaped equalizer UI.
            compose.waitUntil(5_000) { EqualizerControl.state.value.frequencies.isNotEmpty() || EqualizerControl.state.value.error }
            if (!EqualizerControl.state.value.error) {
                compose.runOnIdle {
                    EqualizerControl.setEnabled(true)
                    EqualizerControl.setPreset(EqualizerPreset.ROCK)
                    assertFalse(EqualizerControl.state.value.error)
                    assertEquals("Rock", EqualizerControl.state.value.preset)
                    assertTrue(EqualizerControl.state.value.levels.any { it != 0 })
                    EqualizerControl.setBand(0, 123)
                    assertEquals(123, EqualizerControl.state.value.levels[0])
                    EqualizerControl.setPreset(EqualizerPreset.NORMAL)
                    EqualizerControl.setEnabled(false)
                }
            }
            compose.runOnIdle { model.next() }
            compose.waitUntil(5_000) { model.state.value.current?.id == ids[2] }
        } finally {
            // Stop only this app; all MediaStore rows below were created by this test.
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val store = ViewModelProvider(compose.activity)[MusicViewModel::class.java]
                if (store.state.value.playing) store.togglePlayback()
            }
            uris.forEach { resolver.delete(it, null, null) }
        }
    }
}
