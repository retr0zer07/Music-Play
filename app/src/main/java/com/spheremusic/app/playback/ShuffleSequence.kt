package com.spheremusic.app.playback

import kotlin.random.Random

internal fun shuffleSequence(count: Int, current: Int, random: Random = Random.Default): IntArray {
    if (count == 0) return intArrayOf()
    val start = current.coerceIn(0, count - 1)
    val remaining = (0 until count).filter { it != start }.shuffled(random).toMutableList()
    // Make enabling shuffle visibly different from the next track in the normal queue.
    if (remaining.size > 1 && remaining.first() == (start + 1) % count) {
        val swap = random.nextInt(1, remaining.size)
        val first = remaining[0]
        remaining[0] = remaining[swap]
        remaining[swap] = first
    }
    return (listOf(start) + remaining).toIntArray()
}
