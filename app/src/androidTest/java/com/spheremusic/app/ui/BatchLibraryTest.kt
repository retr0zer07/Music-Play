package com.spheremusic.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.spheremusic.app.MainActivity
import com.spheremusic.app.MusicViewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class BatchLibraryTest {
    @get:Rule(order = 0) val permission = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun selectsMultipleAddsPlaylistCancelsThenConfirmsAndroidBatchDeletion() {
        assumeTrue(Build.VERSION.SDK_INT >= 30)
        val resolver = compose.activity.contentResolver
        val uris = mutableListOf<Uri>()
        val prefix = "SphereBatch${System.nanoTime()}"
        val playlistName = "$prefix Playlist"
        lateinit var model: MusicViewModel
        var playlistId: String? = null
        fun exists(uri: Uri) = checkNotNull(resolver.query(uri, arrayOf("_id"), null, null, null)).use { it.moveToFirst() }
        try {
            repeat(2) { index ->
                val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$prefix-$index.wav")
                    put(MediaStore.Audio.Media.TITLE, "$prefix-$index")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SphereMusicTest")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }))
                uris.add(uri)
                val samples = ByteArray(16_000)
                val wav = ByteBuffer.allocate(44 + samples.size).order(ByteOrder.LITTLE_ENDIAN)
                    .put("RIFF".toByteArray()).putInt(36 + samples.size)
                    .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                    .putInt(8_000).putInt(16_000).putShort(2).putShort(16)
                    .put("data".toByteArray()).putInt(samples.size).put(samples).array()
                checkNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            }
            val ids = uris.map(ContentUris::parseId)
            compose.runOnIdle { model = ViewModelProvider(compose.activity)[MusicViewModel::class.java]; model.refresh() }
            compose.waitUntil(10_000) { model.state.value.songs.count { it.id in ids } == 2 }
            val songs = ids.map { id -> model.state.value.songs.single { it.id == id } }
            compose.onNode(hasSetTextAction()).performTextInput(prefix)
            fun selectBoth() {
                compose.onNodeWithText(songs[0].title).performTouchInput { longClick() }
                compose.onNodeWithText(songs[1].title).performClick()
            }
            selectBoth()
            compose.onNodeWithContentDescription("Agregar a playlist").performClick()
            compose.onNodeWithText("Crear playlist").performClick()
            compose.onNodeWithText("Nombre de la playlist").performTextInput(playlistName)
            compose.onNodeWithText("Guardar").performClick()
            compose.waitUntil(10_000) { model.state.value.playlists.any { it.name == playlistName } }
            val playlist = model.state.value.playlists.single { it.name == playlistName }
            playlistId = playlist.id
            assertEquals(ids.toSet(), playlist.songIds.toSet())
            compose.runOnIdle { model.toggleFavorite(songs[0]) }
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            fun requestBulk() {
                selectBoth()
                compose.onNodeWithContentDescription("Borrar del teléfono").performClick()
                compose.onNodeWithText("Eliminar").performClick()
            }
            requestBulk()
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(?i)deny|cancel|don.t allow"))), 10_000)) {
                "Android cancellation button not found"
            }.click()
            compose.waitForIdle()
            assertTrue(uris.all(::exists))
            assertTrue(model.state.value.favorites.contains(ids[0]))
            assertEquals(2, model.state.value.playlists.single { it.id == playlist.id }.songIds.size)
            requestBulk()
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(?i)allow|delete"))), 10_000)) {
                "Android approval button not found"
            }.click()
            compose.waitUntil(10_000) { model.state.value.songs.none { it.id in ids } }
            assertFalse(uris.any(::exists))
            assertFalse(model.state.value.favorites.contains(ids[0]))
            compose.waitUntil(5_000) { model.state.value.playlists.single { it.id == playlist.id }.songIds.isEmpty() }
        } finally {
            playlistId?.let { id -> compose.runOnIdle { model.deletePlaylist(id) } }
            uris.forEach { resolver.delete(it, null, null) }
        }
    }
}
