package com.spheremusic.app.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class AutomaticLyricsTest {
    @Test fun snapTubeSidecarLoadsFromAuthorizedAudioDirectory() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Opt-in device regression", arguments.getString("verifyDracula") == "true")
        val repository = MusicRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        assertNotNull("Audio folder must have been authorized", repository.lyricsFolder())
        val song = repository.songs().single {
            it.fileName == "Tame Impala - Dracula (Official Video)(M4A_128K).m4a"
        }
        assertTrue("MediaStore folder location must be present", song.relativePath.isNotBlank())
        val lyrics = repository.lyrics(song)
        assertNotNull("SnapTube _private LRC must load automatically", lyrics)
        assertTrue("Parsed lyrics must contain nonblank lines", lyrics!!.lines.any { it.text.isNotBlank() })
        assertTrue("LRC should have timestamps", lyrics.synchronized)
    }

    @Test fun duplicateSidecarsPreferTheirOwnAudioFolder() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Opt-in fixture regression", arguments.getString("verifySidecars") == "true")
        val repository = MusicRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val songs = repository.songs().filter { it.fileName == "SphereSibling0d2c2ce6.wav" }
        assertTrue("Both original test files must be indexed", songs.size == 2)
        for (song in songs) {
            val expected = if (song.relativePath.endsWith("/A/")) "Original sibling A" else "Original sibling B"
            val lyrics = repository.lyrics(song)
            assertTrue("Must select the LRC next to each audio, not the duplicate elsewhere",
                lyrics?.lines?.firstOrNull()?.text == expected)
        }
    }
}
