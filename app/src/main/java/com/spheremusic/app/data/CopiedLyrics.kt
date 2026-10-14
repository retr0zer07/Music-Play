package com.spheremusic.app.data

import java.security.MessageDigest

object CopiedLyrics {
    const val MAX_BYTES = 1_048_576

    fun validate(text: String): Lyrics {
        require(text.length <= MAX_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Lyrics exceed 1 MB"
        }
        return requireNotNull(LyricsParser.parse(text)) { "Empty lyrics" }
    }

    fun fingerprint(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun query(song: Song): String = "lyric ${song.title} ${song.artist}".trim()
}
