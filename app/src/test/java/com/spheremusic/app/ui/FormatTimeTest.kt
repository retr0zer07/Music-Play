package com.spheremusic.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTimeTest {
    @Test fun `formats minutes hours and clamps negative positions`() {
        assertEquals("0:00", formatTime(-1))
        assertEquals("0:00", formatTime(0))
        assertEquals("3:05", formatTime(185_999))
        assertEquals("1:01:01", formatTime(3_661_000))
    }
}
