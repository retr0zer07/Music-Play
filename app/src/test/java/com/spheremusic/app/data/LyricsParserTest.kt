package com.spheremusic.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {
    @Test fun `parses fractions multiple timestamps and sorts lines`() {
        val lyrics = LyricsParser.parse("[ar:Sphere]\n[00:12.50][00:42.500]Estribillo\n[00:02.1]Inicio")!!
        assertTrue(lyrics.synchronized)
        assertEquals(listOf(2100L, 12500L, 42500L), lyrics.lines.map { it.timeMs })
        assertEquals(listOf("Inicio", "Estribillo", "Estribillo"), lyrics.lines.map { it.text })
    }

    @Test fun `applies signed offsets and clamps negative time`() {
        val lyrics = LyricsParser.parse("[offset:-1500]\n[00:01.000]Inicio\n[00:05.000]Final")!!
        assertEquals(listOf(0L, 3500L), lyrics.lines.map { it.timeMs })
    }

    @Test fun `active line is absent before first timestamp`() {
        val lyrics = LyricsParser.parse("[00:02]Uno\n[00:04]Dos")!!
        assertEquals(-1, lyrics.activeIndex(1999))
        assertEquals(0, lyrics.activeIndex(2000))
        assertEquals(1, lyrics.activeIndex(5000))
    }

    @Test fun `supports UTF8 BOM and plain text without metadata`() {
        val lyrics = LyricsParser.parse("\uFEFF[ti:Tema]\r\nHola\r\nMundo")!!
        assertFalse(lyrics.synchronized)
        assertEquals(listOf("Hola", "Mundo"), lyrics.lines.map { it.text })
        assertEquals(-1, lyrics.activeIndex(0))
    }

    @Test fun `rejects empty metadata-only and invalid seconds`() {
        assertNull(LyricsParser.parse(" \n"))
        assertNull(LyricsParser.parse("[ar:Artista]\n[ti:Tema]"))
        assertNull(LyricsParser.parse("[00:75]Inválida"))
    }

    @Test fun `keeps blank timed lines and handles extreme offsets`() {
        val lyrics = LyricsParser.parse("[00:01]Uno\n[00:03]\n[00:05]Dos")!!
        assertEquals("", lyrics.lines[1].text)
        val extreme = LyricsParser.parse("[offset:9223372036854775807]\n[00:01]Uno")!!
        assertEquals(Long.MAX_VALUE, extreme.lines.first().timeMs)
    }
}
