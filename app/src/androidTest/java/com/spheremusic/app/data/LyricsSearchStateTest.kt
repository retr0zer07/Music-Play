package com.spheremusic.app.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.spheremusic.app.MusicViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSearchStateTest {
    @Test fun importIsArmedOnlyForExplicitSearchAndStaysBoundToOriginalSong() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val application = instrumentation.targetContext.applicationContext as Application
            val saved = SavedStateHandle()
            val model = MusicViewModel(application, saved)
            try {
                model.receiveCopiedLyrics("Unrequested original text")
                assertNull(model.state.value.lyricsImport)
                val song = Song(909, Uri.EMPTY, "Original searched track", "Sphere", "", 1, 0, null)
                model.startLyricsSearch(song, "Old original clipboard")
                assertFalse(model.awaitingCopiedLyrics())
                model.receiveCopiedLyrics("Not yet returned")
                assertNull(model.state.value.lyricsImport)
                model.lyricsSearchLeftApp()
                assertTrue(model.awaitingCopiedLyrics())
                model.receiveCopiedLyrics("Old original clipboard")
                assertEquals("", model.state.value.lyricsImport?.text)
                model.cancelLyricsSearch()
                assertFalse(model.awaitingCopiedLyrics())
                model.startLyricsSearch(song, null)
                model.lyricsSearchLeftApp()
                model.receiveCopiedLyrics("New original copied line")
                assertEquals(909L, model.state.value.lyricsImport?.songId)
                assertEquals("Original searched track", model.state.value.lyricsImport?.title)
                assertEquals("New original copied line", model.state.value.lyricsImport?.text)
                model.cancelLyricsSearch()
                assertNull(model.state.value.lyricsImport)
            } finally {
                androidx.lifecycle.ViewModelStore().apply { put("test", model); clear() }
            }
        }
    }
}
