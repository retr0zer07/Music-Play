package com.spheremusic.app.data

object AudioFilter {
    fun isWhatsApp(path: String): Boolean = path.replace('\\', '/').split('/').any {
        it.equals("WhatsApp", true) || it.equals("WhatsApp Business", true) ||
            it.equals("com.whatsapp", true) || it.equals("com.whatsapp.w4b", true) ||
            it.startsWith("WhatsApp ", true)
    }
}
