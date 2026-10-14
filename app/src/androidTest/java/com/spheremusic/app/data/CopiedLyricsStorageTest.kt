package com.spheremusic.app.data

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CopiedLyricsStorageTest {
    @Test fun copiedTextPersistsLinksToOnlySelectedSongAndKeepsPreviousOnInvalidInput() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = -System.nanoTime()
        val song = Song(id, Uri.EMPTY, "Original & test", "Sphere", "", 1, 0, null)
        val repository = MusicRepository(context)
        val prefs = context.getSharedPreferences("sphere_music", android.content.Context.MODE_PRIVATE)
        try {
            repository.saveCopiedLyrics(id, "Original private line\nAnother original line")
            val firstUri = Uri.parse(prefs.getString("lyrics_$id", null))
            assertEquals("file", firstUri.scheme)
            assertTrue(java.io.File(checkNotNull(firstUri.path)).exists())
            assertEquals("Original private line", MusicRepository(context).lyrics(song)?.lines?.first()?.text)
            try {
                repository.saveCopiedLyrics(id, " ")
                fail("Empty text must not overwrite saved lyrics")
            } catch (_: IllegalArgumentException) { }
            assertEquals(firstUri.toString(), prefs.getString("lyrics_$id", null))
            repository.saveCopiedLyrics(id, "[00:01.00]New original line")
            val lyrics = MusicRepository(context).lyrics(song)
            assertEquals("New original line", lyrics?.lines?.single()?.text)
            assertTrue(lyrics?.synchronized == true)
            assertFalse(java.io.File(checkNotNull(firstUri.path)).exists())
            assertEquals("lyric Original & test Sphere", CopiedLyrics.query(song))
            val search = Uri.Builder().scheme("https").authority("www.google.com").path("search")
                .appendQueryParameter("q", CopiedLyrics.query(song)).build()
            assertEquals(CopiedLyrics.query(song), search.getQueryParameter("q"))
        } finally {
            repository.forgetSong(id)
            assertFalse(prefs.contains("lyrics_$id"))
        }
    }
}
