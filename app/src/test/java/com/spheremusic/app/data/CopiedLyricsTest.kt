package com.spheremusic.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiedLyricsTest {
    @Test fun plainAndTimedTextRetainTheirMeaning() {
        val plain = CopiedLyrics.validate("Original line\nAnother original line")
        assertFalse(plain.synchronized)
        assertEquals(2, plain.lines.size)
        val timed = CopiedLyrics.validate("[00:02.00]Original timed line")
        assertTrue(timed.synchronized)
        assertEquals(2_000, timed.lines.single().timeMs)
    }

    @Test fun acceptsExactUtf8Limit() {
        assertEquals(CopiedLyrics.MAX_BYTES, CopiedLyrics.validate("a".repeat(CopiedLyrics.MAX_BYTES)).lines.single().text.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooManyBytesEvenWhenCharacterCountIsLower() {
        CopiedLyrics.validate("\u00e9".repeat(CopiedLyrics.MAX_BYTES / 2 + 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyText() { CopiedLyrics.validate(" \n ") }

    @Test fun fingerprintDetectsChangedTextWithoutRetainingClipboardContent() {
        assertEquals(CopiedLyrics.fingerprint("original"), CopiedLyrics.fingerprint("original"))
        assertFalse(CopiedLyrics.fingerprint("original") == CopiedLyrics.fingerprint("changed"))
        assertEquals(64, CopiedLyrics.fingerprint("original").length)
    }
}
