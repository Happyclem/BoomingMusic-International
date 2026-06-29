package com.mardous.booming.data.local.lyrics

import com.mardous.booming.core.model.lyrics.TranslationFilter
import com.mardous.booming.core.model.lyrics.TranslationFilter.Companion.toValue
import com.mardous.booming.data.local.lyrics.ttml.TtmlLyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the F2 step-2 logic: available-language extraction on [SyncedLyrics] and the
 * [TranslationFilter] selection model.
 */
class TranslationFilterTest {

    private fun ttml(body: String) = """<?xml version="1.0" encoding="UTF-8"?>
<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
<body><div>
$body
</div></body></tt>"""

    private fun parse(body: String) = TtmlLyricsParser().parse(ttml(body), 60_000, false)!!

    @Test
    fun `available languages are distinct and in first-seen order`() {
        val lyrics = parse(
            """<p begin="00:00:01.000" end="00:00:03.000" itunes:key="L1">One<span ttm:role="x-translation" xml:lang="fr">Un</span><span ttm:role="x-translation" xml:lang="es">Uno</span></p>
<p begin="00:00:03.000" end="00:00:05.000" itunes:key="L2">Two<span ttm:role="x-translation" xml:lang="fr">Deux</span><span ttm:role="x-translation" xml:lang="es">Dos</span></p>"""
        )

        assertEquals(listOf("fr", "es"), lyrics.availableTranslationLanguages)
        assertTrue(lyrics.hasTranslations)
        assertFalse(lyrics.hasUntaggedTranslations)
    }

    @Test
    fun `lyrics without translations report none`() {
        val lyrics = parse(
            """<p begin="00:00:01.000" end="00:00:03.000" itunes:key="L1">Only original</p>"""
        )

        assertTrue(lyrics.availableTranslationLanguages.isEmpty())
        assertFalse(lyrics.hasTranslations)
    }

    @Test
    fun `filter selects, hides or keeps all translation layers`() {
        val line = parse(
            """<p begin="00:00:01.000" end="00:00:03.000" itunes:key="L1">One<span ttm:role="x-translation" xml:lang="fr">Un</span><span ttm:role="x-translation" xml:lang="es">Uno</span></p>"""
        ).lines.single { !it.isEmpty }

        assertEquals(2, TranslationFilter.All.apply(line.translations).size)
        assertEquals(0, TranslationFilter.Off.apply(line.translations).size)

        val onlyFr = TranslationFilter.Language("fr").apply(line.translations)
        assertEquals(listOf("fr"), onlyFr.map { it.lang })
        assertEquals("Un", onlyFr.single().content.content)
    }

    @Test
    fun `filter round-trips through its serialized value`() {
        assertEquals(TranslationFilter.All, TranslationFilter.fromValue("all"))
        assertEquals(TranslationFilter.All, TranslationFilter.fromValue(null))
        assertEquals(TranslationFilter.Off, TranslationFilter.fromValue("off"))
        assertEquals(TranslationFilter.Language("fr"), TranslationFilter.fromValue("fr"))

        assertEquals("all", TranslationFilter.All.toValue())
        assertEquals("off", TranslationFilter.Off.toValue())
        assertEquals("fr", TranslationFilter.Language("fr").toValue())
    }
}
