package com.spheremusic.app.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaylistStoreTest {
    @Test fun playlistsPersistDeduplicateAndRemoveDeletedSongReferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("playlist-test-${System.nanoTime()}", Context.MODE_PRIVATE)
        try {
            val store = PlaylistStore(preferences)
            val first = store.create("  Road trip  ", 11).single()
            store.addSong(first.id, 12)
            store.addSong(first.id, 11)
            assertEquals(listOf(11L, 12L), PlaylistStore(preferences).load().single().songIds)
            val second = store.create("Favorites", 11).last()
            store.rename(first.id, "Driving")
            assertEquals("Driving", store.load().first().name)
            store.removeSong(first.id, 12)
            assertEquals(listOf(11L), store.load().first().songIds)
            store.removeSongEverywhere(11)
            assertTrue(store.load().all { it.songIds.isEmpty() })
            store.delete(second.id)
            assertEquals(listOf(first.id), store.load().map { it.id })
            for (name in listOf(" ", "driving")) {
                try { store.create(name); fail("Must reject blank or duplicate name") }
                catch (_: IllegalArgumentException) { }
            }
            assertEquals(1, store.load().size)
        } finally {
            assertTrue(preferences.edit().clear().commit())
        }
    }
}
