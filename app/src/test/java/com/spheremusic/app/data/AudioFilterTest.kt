package com.spheremusic.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFilterTest {
    @Test fun excludesLegacyModernAndBusinessWhatsAppFoldersOnly() {
        listOf(
            "WhatsApp/Media/WhatsApp Voice Notes/2026/",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Audio/",
            "/storage/emulated/0/whatsapp/media/audio/",
        ).forEach { assertTrue(it, AudioFilter.isWhatsApp(it)) }
        listOf("", "Music/", "Music/WhatsAppMix/", "Music/NotWhatsApp/", "snaptube/download/SnapTube Audio/")
            .forEach { assertFalse(it, AudioFilter.isWhatsApp(it)) }
    }
}
