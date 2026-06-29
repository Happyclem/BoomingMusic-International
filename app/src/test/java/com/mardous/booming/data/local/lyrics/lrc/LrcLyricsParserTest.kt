package com.mardous.booming.data.local.lyrics.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LrcLyricsParser], focused on the multiple-translation behavior (F1).
 *
 * Placeholder text only — not real lyrics.
 */
class LrcLyricsParserTest {

    private val parser = LrcLyricsParser()

    private fun parse(content: String) =
        parser.parse(content, trackLength = 60_000, ignoreBlankLines = false)

    @Test
    fun `single line without translation parses as original only`() {
        val lyrics = parse("[00:10.00]Example line here")
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals("Example line here", line.content.content)
        assertTrue(line.translations.isEmpty())
        assertNull(line.translation)
    }

    @Test
    fun `one extra line at same timestamp is a single translation`() {
        val lyrics = parse(
            """
            [00:10.00]Example line here
            [00:10.00]Exemple de ligne traduite
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals("Example line here", line.content.content)
        assertEquals(1, line.translations.size)
        assertEquals("Exemple de ligne traduite", line.translations[0].content.content)
        // Backward-compatible alias still resolves to the first translation.
        assertEquals("Exemple de ligne traduite", line.translation?.content)
    }

    @Test
    fun `multiple extra lines at same timestamp stack as ordered translations`() {
        val lyrics = parse(
            """
            [00:10.00]Example line here
            [00:10.00]Exemple de ligne traduite
            [00:10.00]Ejemplo de linea traducida
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals("Example line here", line.content.content)
        assertEquals(
            listOf("Exemple de ligne traduite", "Ejemplo de linea traducida"),
            line.translations.map { it.content.content }
        )
    }

    @Test
    fun `duplicate translation lines are not added twice`() {
        val lyrics = parse(
            """
            [00:10.00]Example line here
            [00:10.00]Exemple de ligne traduite
            [00:10.00]Exemple de ligne traduite
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals(1, line.translations.size)
    }

    @Test
    fun `translation lines have no language tag in LRC`() {
        val lyrics = parse(
            """
            [00:10.00]Example line here
            [00:10.00]Exemple de ligne traduite
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertNull(line.translations[0].lang)
    }

    @Test
    fun `word-synced original keeps its syllables while translation stays plain`() {
        val lyrics = parse(
            """
            [00:10.00]<00:10.00>Example <00:10.40>word <00:10.80>here
            [00:10.00]Exemple de ligne traduite
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertTrue("original should be word-synced", line.content.isWordSynced)
        assertEquals(1, line.translations.size)
        assertTrue(
            "translation layer should be plain text",
            line.translations[0].content.syllables.isEmpty()
        )
    }

    @Test
    fun `plain original followed by word-synced line promotes the word-synced one`() {
        // Mirrors the existing swap heuristic: the word-synced line becomes the main content
        // and the plain line is demoted to the translation layer.
        val lyrics = parse(
            """
            [00:10.00]Exemple de ligne traduite
            [00:10.00]<00:10.00>Example <00:10.40>word <00:10.80>here
            """.trimIndent()
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertTrue("promoted original should be word-synced", line.content.isWordSynced)
        assertEquals(1, line.translations.size)
        assertEquals("Exemple de ligne traduite", line.translations[0].content.content)
    }
}
