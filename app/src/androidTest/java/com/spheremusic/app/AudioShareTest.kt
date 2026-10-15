package com.spheremusic.app

import android.content.Intent
import android.net.Uri
import com.spheremusic.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioShareTest {
    @Suppress("DEPRECATION")
    @Test fun sharesSingleAndMultipleFilesWithReadGrantsForEveryUri() {
        val songs = (1L..3L).map {
            Song(it, Uri.parse("content://media/external/audio/media/$it"), "Original $it", "Sphere", "", it, 1000, null)
        }
        val single = audioShareIntent(listOf(songs[0]))
        assertEquals(Intent.ACTION_SEND, single.action)
        assertEquals(songs[0].uri, single.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        val multiple = audioShareIntent(songs + songs[0])
        assertEquals(Intent.ACTION_SEND_MULTIPLE, multiple.action)
        assertEquals(songs.map { it.uri }, multiple.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
        listOf(single, multiple).forEach { intent ->
            assertEquals("audio/*", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            val clip = checkNotNull(intent.clipData)
            assertEquals(if (intent === single) 1 else 3, clip.itemCount)
            repeat(clip.itemCount) { assertEquals(songs[it].uri, clip.getItemAt(it).uri) }
        }
    }
}
