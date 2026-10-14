package com.spheremusic.app.data

import java.text.Normalizer
import java.util.Locale

object LyricsMatcher {
    fun key(name: String): String = Normalizer.normalize(
        name.trim().replace(Regex("""(?i)\.(lrc|mp3|m4a|flac|wav|ogg|opus|aac|wma|aiff|alac|amr|3gp)$"""), ""),
        Normalizer.Form.NFC,
    ).lowercase(Locale.ROOT)

    fun <T> match(fileName: String, title: String, index: Map<String, List<T>>): T? {
        for (name in listOf(fileName, title).filter { it.isNotBlank() }.distinctBy(::key)) {
            for (candidate in listOf(key(name), "${key(name)}_private")) {
                val matches = index[candidate].orEmpty()
                if (matches.size > 1) throw AmbiguousLyricsException(name)
                matches.singleOrNull()?.let { return it }
            }
        }

        return null
    }
}

class AmbiguousLyricsException(name: String) : IllegalArgumentException("Multiple LRC files match $name")
