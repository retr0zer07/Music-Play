package com.spheremusic.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkColorsTest {
    @Test fun findsDominantPixelsInsteadOfAveragingOpposingColors() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.RED)
            for (x in 0..2) for (y in 0..9) bitmap.setPixel(x, y, Color.BLUE)
            val result = checkNotNull(dominantColor(bitmap))
            assertTrue(Color.red(result) >= 248)
            assertTrue(Color.blue(result) <= 8)
        } finally { bitmap.recycle() }
    }
}
