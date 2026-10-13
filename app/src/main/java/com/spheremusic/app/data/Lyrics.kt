package com.spheremusic.app.data

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(val lines: List<LyricLine>, val synchronized: Boolean) {
    fun activeIndex(positionMs: Long): Int {
        if (!synchronized) return -1
        return lines.indexOfLast { it.timeMs <= positionMs }
    }
}

object LyricsParser {
    private val timestamp = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val offset = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)
    private val metadata = Regex("""^\[(ar|al|ti|au|by|re|ve|length|offset):.*]$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): Lyrics? {
        val clean = text.removePrefix("\uFEFF").replace("\r", "")
        val offsetMs = offset.find(clean)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val timed = mutableListOf<LyricLine>()
        val plain = mutableListOf<LyricLine>()
        clean.lines().forEach { raw ->
            val matches = timestamp.findAll(raw).toList()
            val words = timestamp.replace(raw, "").trim()
            if (matches.isNotEmpty()) {
                matches.forEach timestampLoop@{ match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@timestampLoop
                    val seconds = match.groupValues[2].toLong()
                    if (seconds >= 60 || minutes > 1_000_000) return@timestampLoop
                    val fraction = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0
                    val time = minutes * 60_000 + seconds * 1_000 + fraction
                    val adjusted = if (offsetMs > 0 && time > Long.MAX_VALUE - offsetMs) {
                        Long.MAX_VALUE
                    } else {
                        (time + offsetMs).coerceAtLeast(0)
                    }
                    timed += LyricLine(adjusted, words)
                }
            } else if (words.isNotBlank() && !metadata.matches(words)) {
                plain += LyricLine(0, words)
            }
        }
        return when {
            timed.any { it.text.isNotBlank() } -> Lyrics(timed.sortedBy { it.timeMs }, true)
            plain.isNotEmpty() -> Lyrics(plain, false)
            else -> null
        }
    }
}
