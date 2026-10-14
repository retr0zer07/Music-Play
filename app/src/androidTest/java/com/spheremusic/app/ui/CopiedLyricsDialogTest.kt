package com.spheremusic.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.spheremusic.app.LyricsImport
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CopiedLyricsDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun previewsTargetSongAndExplicitPasteCanReplaceEmptyClipboardPreview() {
        var saved: String? = null
        compose.setContent {
            SphereTheme {
                CopiedLyricsDialog(LyricsImport(7, "Original track", ""), false, {},
                    { "Original copied text" }, { saved = it })
            }
        }
        compose.onNodeWithText("Guardar letra de Original track").assertExists()
        compose.onNodeWithText("Pegar texto copiado").performClick()
        compose.onNodeWithText("Original copied text").assertExists()
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertEquals("Original copied text", saved) }
    }

    @Test fun cancelDoesNotSaveCopiedText() {
        var cancelled = false
        var saved = false
        compose.setContent {
            SphereTheme {
                CopiedLyricsDialog(LyricsImport(8, "Another original", "Original line"), false,
                    { cancelled = true }, { null }, { saved = true })
            }
        }
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(true, cancelled); assertEquals(false, saved) }
    }
}
