package com.spheremusic.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsMatcherTest {
    @Test fun matchesSnapTubePrivateSidecarButExactNameWins() {
        val audio = "Tame Impala - Dracula (Official Video)(M4A_128K).m4a"
        val privateKey = LyricsMatcher.key("Tame Impala - Dracula (Official Video)(M4A_128K)_private.lrc")
        assertEquals("private", LyricsMatcher.match(audio, "Dracula", mapOf(privateKey to listOf("private"))))
        assertEquals("exact", LyricsMatcher.match(audio, "Dracula",
            mapOf(privateKey to listOf("private"), LyricsMatcher.key(audio) to listOf("exact"))))
    }

    @Test fun `matches audio filename case insensitively and normalizes Unicode`() {
        val index = mapOf(LyricsMatcher.key("Canción.LRC") to listOf("uri"))
        assertEquals("uri", LyricsMatcher.match("CANCIO\u0301N.mp3", "Other", index))
    }

    @Test fun `filename wins over title and title is fallback`() {
        val index = mapOf("file" to listOf("file-uri"), "title" to listOf("title-uri"))
        assertEquals("file-uri", LyricsMatcher.match("file.flac", "title", index))
        assertEquals("title-uri", LyricsMatcher.match("missing.m4a", "title", index))
        assertNull(LyricsMatcher.match("missing.mp3", "missing", index))
    }

    @Test(expected = AmbiguousLyricsException::class)
    fun `duplicate basenames must not choose unrelated lyrics`() {
        LyricsMatcher.match("file.mp3", "title", mapOf("file" to listOf("one", "two")))
    }

    @Test fun `does not strip dots from titles or match partial names`() {
        assertEquals("chapter.2", LyricsMatcher.key("Chapter.2.lrc"))
        assertNull(LyricsMatcher.match("song.mp3", "song", mapOf("song remix" to listOf("remix"))))
    }
}
