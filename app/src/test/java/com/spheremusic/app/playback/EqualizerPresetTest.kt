package com.spheremusic.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetTest {
    @Test fun interpolatesPresetsAcrossHardwareBandCounts() {
        for (preset in EqualizerPreset.entries) for (count in listOf(1, 3, 5, 10, 32)) {
            for (band in 0 until count) assertTrue(preset.level(band, count) in -1500..1500)
            if (count > 1) {
                assertEquals(preset.curve.first(), preset.level(0, count))
                assertEquals(preset.curve.last(), preset.level(count - 1, count))
            }
        }
    }
}
