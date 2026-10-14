package com.spheremusic.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsLocationTest {
    private val authority = "com.android.externalstorage.documents"

    @Test fun mapsAudioFolderWithinAuthorizedParent() {
        assertEquals("primary:snaptube/download/SnapTube Audio",
            LyricsLocation.siblingDirectory(authority, "primary:snaptube/download",
                "external_primary", "snaptube/download/SnapTube Audio/"))
    }

    @Test fun allowsExactFolderAndSecondaryVolumes() {
        assertEquals("ABCD-1234:Music/Album",
            LyricsLocation.siblingDirectory(authority, "ABCD-1234:Music/Album",
                "abcd-1234", "Music/Album/"))
    }

    @Test fun doesNotEscapeGrantedFolderOrCrossVolumes() {
        assertNull(LyricsLocation.siblingDirectory(authority, "primary:Music", "external_primary", "Music2/"))
        assertNull(LyricsLocation.siblingDirectory(authority, "primary:Music", "external_primary", "Music/../Other/"))
        assertNull(LyricsLocation.siblingDirectory(authority, "primary:Music", "ABCD-1234", "Music/"))
        assertNull(LyricsLocation.siblingDirectory("cloud.provider", "primary:Music", "external_primary", "Music/"))
        assertNull(LyricsLocation.siblingDirectory(authority, "primary:Music", "", ""))
    }
}
