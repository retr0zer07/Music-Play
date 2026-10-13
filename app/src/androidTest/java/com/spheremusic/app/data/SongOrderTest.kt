package com.spheremusic.app.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class SongOrderTest {
    @Test fun recentIsDefaultAndDateTiesAreDeterministic() {
        val old = Song(1, Uri.EMPTY, "1 old", "Sphere", "", 1, 0, null, "old.mp3", 10)
        val recent = old.copy(id = 2, title = "Zulu", dateAddedSeconds = 20)
        val tie = recent.copy(id = 3, title = "Alpha")
        assertEquals(listOf(3L, 2L, 1L), SongOrder.RECENT.sorted(listOf(old, recent, tie)).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), SongOrder.TITLE.sorted(listOf(recent, old, tie)).map { it.id })
    }
}
