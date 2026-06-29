package com.mardous.booming.data.local.lyrics.ttml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TtmlLyricsParser], focused on collecting multiple `x-translation` layers (F1).
 *
 * Placeholder text only — not real lyrics.
 */
class TtmlLyricsParserTest {

    private val parser = TtmlLyricsParser()

    private fun parse(content: String) =
        parser.parse(content, trackLength = 60_000, ignoreBlankLines = false)

    // Note: the XML declaration must be the very first character, so the result is trimmed to
    // drop the leading newline introduced by the raw-string literal.
    private fun ttml(body: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tt xmlns="http://www.w3.org/ns/ttml"
            xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
            xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
          <body>
            <div>
              $body
            </div>
          </body>
        </tt>
    """.trimIndent().trim()

    @Test
    fun `line with two x-translation spans keeps both, in document order`() {
        val lyrics = parse(
            ttml(
                """
                <p begin="00:00:10.000" end="00:00:15.000" itunes:key="L1">
                  <span begin="00:00:10.000" end="00:00:10.400">Example </span>
                  <span begin="00:00:10.400" end="00:00:10.800">word </span>
                  <span begin="00:00:10.800" end="00:00:15.000">here</span>
                  <span ttm:role="x-translation" xml:lang="fr">Exemple de ligne traduite</span>
                  <span ttm:role="x-translation" xml:lang="es">Ejemplo de linea traducida</span>
                </p>
                """.trimIndent()
            )
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals("Example word here", line.content.content)
        assertEquals(2, line.translations.size)
        assertEquals(
            setOf("fr", "es"),
            line.translations.mapNotNull { it.lang }.toSet()
        )
        assertEquals(
            setOf("Exemple de ligne traduite", "Ejemplo de linea traducida"),
            line.translations.map { it.content.content }.toSet()
        )
    }

    @Test
    fun `consecutive lines each keep their own two translations`() {
        // Regression test: two lines that both carry fr+es inline translations must not lose a
        // layer nor leak one line's translation into the next line's original content.
        val lyrics = parse(
            ttml(
                """
                <p begin="00:00:10.000" end="00:00:12.000" itunes:key="L1">Example one<span ttm:role="x-translation" xml:lang="fr">Exemple un</span><span ttm:role="x-translation" xml:lang="es">Ejemplo uno</span></p>
                <p begin="00:00:12.000" end="00:00:14.000" itunes:key="L2">Example two<span ttm:role="x-translation" xml:lang="fr">Exemple deux</span><span ttm:role="x-translation" xml:lang="es">Ejemplo dos</span></p>
                """.trimIndent()
            )
        )
        checkNotNull(lyrics)

        val first = lyrics.lines.single { it.start == 10_000L }
        assertEquals("Example one", first.content.content)
        assertEquals(listOf("fr", "es"), first.translations.map { it.lang })
        assertEquals(
            listOf("Exemple un", "Ejemplo uno"),
            first.translations.map { it.content.content }
        )

        val second = lyrics.lines.single { it.start == 12_000L }
        assertEquals("Example two", second.content.content)
        assertEquals(listOf("fr", "es"), second.translations.map { it.lang })
        assertEquals(
            listOf("Exemple deux", "Ejemplo dos"),
            second.translations.map { it.content.content }
        )
    }

    @Test
    fun `line with single translation still resolves the alias`() {
        val lyrics = parse(
            ttml(
                """
                <p begin="00:00:10.000" end="00:00:15.000" itunes:key="L1">
                  <span begin="00:00:10.000" end="00:00:15.000">Example line here</span>
                  <span ttm:role="x-translation" xml:lang="fr">Exemple de ligne traduite</span>
                </p>
                """.trimIndent()
            )
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertEquals(1, line.translations.size)
        assertEquals("Exemple de ligne traduite", line.translation?.content)
    }

    @Test
    fun `line with no translation has an empty translations list`() {
        val lyrics = parse(
            ttml(
                """
                <p begin="00:00:10.000" end="00:00:15.000" itunes:key="L1">
                  <span begin="00:00:10.000" end="00:00:15.000">Example line here</span>
                </p>
                """.trimIndent()
            )
        )
        checkNotNull(lyrics)

        val line = lyrics.lines.single { it.start == 10_000L }
        assertTrue(line.translations.isEmpty())
    }
}
