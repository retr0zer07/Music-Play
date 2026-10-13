package com.spheremusic.app.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal fun InputStream.readUpTo(limit: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(limit, 8192))
    val buffer = ByteArray(minOf(limit, 8192))
    while (output.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count == -1) break
        if (count == 0) {
            val byte = read()
            if (byte == -1) break
            output.write(byte)
        } else {
            output.write(buffer, 0, count)
        }
    }
    return output.toByteArray()
}
