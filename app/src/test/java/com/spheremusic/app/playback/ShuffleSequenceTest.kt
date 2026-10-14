package com.spheremusic.app.playback

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleSequenceTest {
    @Test fun sequencePreservesCurrentAndVisitsEveryOtherTrackOnce() {
        for (count in 3..20) for (current in 0 until count) for (seed in 0..20) {
            val order = shuffleSequence(count, current, Random(seed))
            assertEquals(current, order.first())
            assertEquals((0 until count).toSet(), order.toSet())
            assertEquals(count, order.size)
            assertFalse(order[1] == (current + 1) % count)
        }
    }

    @Test fun emptyAndSmallQueuesWork() {
        assertTrue(shuffleSequence(0, -1).isEmpty())
        assertTrue(shuffleSequence(1, 0).contentEquals(intArrayOf(0)))
        assertTrue(shuffleSequence(2, 1).contentEquals(intArrayOf(1, 0)))
    }
}
