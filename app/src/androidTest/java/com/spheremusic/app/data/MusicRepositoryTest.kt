package com.spheremusic.app.data

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicRepositoryTest {
    @get:Rule
    val audioPermission: GrantPermissionRule = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    @Test
    fun includesAudioNotClassifiedAsMusicWithoutLimitingLibrary() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "Sphere-Test-${System.nanoTime()}.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SphereMusicTest")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = checkNotNull(resolver.insert(collection, values))
        try {
            // A one-second silent PCM WAV, created only for this test.
            val samples = ByteArray(16_000)
            val wav = java.nio.ByteBuffer.allocate(44 + samples.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".toByteArray()).putInt(36 + samples.size)
                .put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(8_000).putInt(16_000).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples.size).put(samples).array()
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(wav) }
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null)
            val songs = MusicRepository(context).songs()
            assertTrue("Audio with IS_MUSIC=0 must be included", songs.any { it.uri == uri })
            val indexedCount = checkNotNull(resolver.query(
                collection, arrayOf(MediaStore.Audio.Media._ID), null, null, null,
            )).use { it.count }
            assertEquals("Every accessible indexed audio row must be returned", indexedCount, songs.size)
            assertTrue("MediaStore query must return recent additions first", songs.zipWithNext().all { (a, b) ->
                a.dateAddedSeconds > b.dateAddedSeconds ||
                    (a.dateAddedSeconds == b.dateAddedSeconds && a.id >= b.id)
            })
        } finally {
            resolver.delete(uri, null, null)
        }
    }
}
