package com.spheremusic.app.data

import java.io.InputStream
import java.nio.charset.Charset

/** Reads bounded ID3v2.3/v2.4 USLT frames without loading the audio into memory. */
object EmbeddedLyrics {
    fun read(stream: InputStream): String? {
        val header = stream.readUpTo(10)
        if (header.size != 10 || String(header, 0, 3, Charsets.ISO_8859_1) != "ID3") return null
        val version = header[3].toInt()
        if (version !in 3..4) return null
        val size = synchsafe(header, 6)
        if (size !in 1..2_097_152) return null
        var tag = stream.readUpTo(size)
        if (tag.size != size) return null
        // v2.3 unsynchronization covers the entire tag, before frame parsing.
        if (version == 3 && header[5].toInt() and 0x80 != 0) tag = deUnsync(tag)
        var cursor = 0
        if (header[5].toInt() and 0x40 != 0) {
            if (tag.size < 4) return null
            val extended = if (version == 4) synchsafe(tag, 0) else integer(tag, 0)
            cursor = if (version == 4) extended else extended + 4
            if (cursor < 4 || cursor > tag.size) return null
        }
        while (cursor + 10 <= tag.size) {
            val id = String(tag, cursor, 4, Charsets.ISO_8859_1)
            val frameSize = if (version == 4) synchsafe(tag, cursor + 4) else integer(tag, cursor + 4)
            if (frameSize <= 0 || frameSize > tag.size - cursor - 10) break
            val flags = tag[cursor + 9].toInt() and 0xff
            val unsupported = if (version == 3) flags and 0xe0 != 0 else flags and 0x4d != 0
            if (id == "USLT" && !unsupported) {
                var frame = tag.copyOfRange(cursor + 10, cursor + 10 + frameSize)
                if (version == 4 && (flags and 0x02 != 0 || header[5].toInt() and 0x80 != 0)) {
                    frame = deUnsync(frame)
                }
                decodeUslt(frame)?.takeIf { it.isNotBlank() }?.let { return it }
            }
            cursor += 10 + frameSize
        }
        return null
    }

    private fun decodeUslt(frame: ByteArray): String? {
        if (frame.size < 5) return null
        val encoding = frame[0].toInt()
        val charset: Charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> return null
        }
        val width = if (encoding in 1..2) 2 else 1
        var cursor = 4
        while (cursor + width <= frame.size) {
            if (frame[cursor] == 0.toByte() && (width == 1 || frame[cursor + 1] == 0.toByte())) {
                return String(frame, cursor + width, frame.size - cursor - width, charset).trimEnd('\u0000')
            }
            cursor += width
        }
        return null
    }

    private fun integer(bytes: ByteArray, start: Int): Int =
        (0..3).fold(0) { value, index -> (value shl 8) or (bytes[start + index].toInt() and 0xff) }

    private fun synchsafe(bytes: ByteArray, start: Int): Int {
        if ((0..3).any { bytes[start + it].toInt() and 0x80 != 0 }) return -1
        return (0..3).fold(0) { value, index -> (value shl 7) or (bytes[start + index].toInt() and 0x7f) }
    }

    private fun deUnsync(bytes: ByteArray): ByteArray {
        val output = ByteArray(bytes.size)
        var index = 0
        var length = 0
        while (index < bytes.size) {
            output[length++] = bytes[index]
            if (bytes[index] == 0xff.toByte() && index + 1 < bytes.size && bytes[index + 1] == 0.toByte()) index++
            index++
        }
        return output.copyOf(length)
    }
}
