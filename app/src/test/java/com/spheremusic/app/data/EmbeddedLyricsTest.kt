package com.spheremusic.app.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbeddedLyricsTest {
    private fun synchsafe(value: Int) = byteArrayOf(
        (value shr 21 and 0x7f).toByte(), (value shr 14 and 0x7f).toByte(),
        (value shr 7 and 0x7f).toByte(), (value and 0x7f).toByte(),
    )

    private fun tag(version: Int, body: ByteArray): ByteArray =
        "ID3".toByteArray() + byteArrayOf(version.toByte(), 0, 0) + synchsafe(body.size) + body

    private fun frame(version: Int, content: ByteArray): ByteArray {
        val size = if (version == 4) synchsafe(content.size) else byteArrayOf(
            (content.size shr 24).toByte(), (content.size shr 16).toByte(),
            (content.size shr 8).toByte(), content.size.toByte(),
        )
        return "USLT".toByteArray() + size + byteArrayOf(0, 0) + content
    }

    @Test fun `reads v24 UTF8 lyrics after description`() {
        val content = byteArrayOf(3) + "spa".toByteArray() + "Descripción".toByteArray() +
            byteArrayOf(0) + "[00:01]Mi universo".toByteArray()
        assertEquals("[00:01]Mi universo", EmbeddedLyrics.read(ByteArrayInputStream(tag(4, frame(4, content)))))
    }

    @Test fun `reads v23 UTF16 lyrics with aligned terminator`() {
        val content = byteArrayOf(1) + "spa".toByteArray() + "Letra".toByteArray(Charsets.UTF_16) +
            byteArrayOf(0, 0) + "Música local".toByteArray(Charsets.UTF_16)
        assertEquals("Música local", EmbeddedLyrics.read(ByteArrayInputStream(tag(3, frame(3, content)))))
    }

    @Test fun `ignores unsupported tags truncated frames and oversized tags`() {
        assertNull(EmbeddedLyrics.read(ByteArrayInputStream("Not an MP3".toByteArray())))
        assertNull(EmbeddedLyrics.read(ByteArrayInputStream(tag(2, ByteArray(20)))))
        val truncated = tag(4, frame(4, byteArrayOf(3, 115, 112, 97, 0, 65))).dropLast(1).toByteArray()
        assertNull(EmbeddedLyrics.read(ByteArrayInputStream(truncated)))
        val oversized = "ID3".toByteArray() + byteArrayOf(4, 0, 0) + synchsafe(3_000_000)
        assertNull(EmbeddedLyrics.read(ByteArrayInputStream(oversized)))
    }

    @Test fun `bounded stream reader handles short streams`() {
        assertEquals("abc", String(ByteArrayInputStream("abc".toByteArray()).readUpTo(100)))
        assertEquals("ab", String(ByteArrayInputStream("abc".toByteArray()).readUpTo(2)))
    }
}
