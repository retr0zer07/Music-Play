package com.spheremusic.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.rule.GrantPermissionRule
import com.spheremusic.app.MainActivity
import com.spheremusic.app.MusicViewModel
import com.spheremusic.app.data.MusicRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CopiedLyricsReturnTest {
    @get:Rule(order = 0) val permission = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun foregroundReturnPreviewsCopiedTextAndSavePersistsForOriginalSong() = runBlocking {
        val context = compose.activity.applicationContext
        val resolver = context.contentResolver
        val name = "Sphere-Copied-Test-${System.nanoTime()}.wav"
        val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SphereMusicTest")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }))
        val repository = MusicRepository(context)
        val id = ContentUris.parseId(uri)
        lateinit var model: MusicViewModel
        var cleanupModel: MusicViewModel? = null
        try {
            val samples = ByteArray(16_000)
            val wav = ByteBuffer.allocate(44 + samples.size).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".toByteArray()).putInt(36 + samples.size)
                .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(8_000).putInt(16_000).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples.size).put(samples).array()
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
            if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null)
            compose.runOnIdle {
                model = ViewModelProvider(compose.activity)[MusicViewModel::class.java]
                cleanupModel = model
                model.refresh()
            }
            compose.waitUntil(10_000) { model.state.value.songs.any { it.id == id } }
            val song = model.state.value.songs.single { it.id == id }
            compose.runOnIdle {
                model.startLyricsSearch(song, "Old original test clipboard")
                compose.activity.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Sphere original test", "Original copied return line"))
            }
            compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitUntil(10_000) { model.state.value.lyricsImport != null }
            compose.onNodeWithText("Guardar letra de ${song.title}").assertExists()
            compose.onNodeWithText("Original copied return line").assertExists()
            compose.runOnIdle { assertEquals(id, model.state.value.lyricsImport?.songId) }
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(10_000) { model.state.value.lyricsImport == null && !model.state.value.lyricsSaving }
            assertEquals("Original copied return line", MusicRepository(context).lyrics(song)?.lines?.single()?.text)
            compose.runOnIdle {
                assertNull(model.state.value.lyricsImport)
                model.receiveCopiedLyrics("Unrequested later original text")
                assertNull(model.state.value.lyricsImport)
            }
        } finally {
            compose.runOnIdle {
                cleanupModel?.cancelLyricsSearch()
                val clipboard = compose.activity.getSystemService(ClipboardManager::class.java)
                if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            repository.forgetSong(id)
            resolver.delete(uri, null, null)
        }
    }
}
