package com.mardous.booming.data.local.lyrics

import com.mardous.booming.data.local.lyrics.ttml.TtmlLyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the real Bones TTML sample file (user-provided original + FR + ES x-translation spans) to
 * confirm the F1 parser handles a full-length, real-world file with two inline translations per
 * line: every line keeps both translation layers in document order and content is non-empty.
 */
class BonesTtmlFileTest {

    private val file = File("../../samples/multilingual-lyrics/Bones - Imagine Dragons.ttml")

    @Test
    fun `bones ttml stacks two translations per line`() {
        assertTrue("sample file missing: ${file.absolutePath}", file.exists())

        val lyrics = TtmlLyricsParser().parse(
            file.readText(),
            trackLength = 165_000,
            ignoreBlankLines = false
        )
        checkNotNull(lyrics) { "parser returned null for the Bones TTML file" }

        // 49 <p> lines in the file (a leading blank offset line may be prepended by the parser).
        val contentLines = lyrics.lines.filter { !it.isEmpty }
        assertEquals(49, contentLines.size)

        // Every content line must carry both translation layers (fr then es), in document order.
        contentLines.forEachIndexed { index, line ->
            assertEquals(
                "line $index ('${line.content.content}') should have 2 translations",
                2,
                line.translations.size
            )
            assertEquals(listOf("fr", "es"), line.translations.map { it.lang })
            line.translations.forEach {
                assertTrue(
                    "translation text should be non-empty on line $index",
                    it.content.content.isNotBlank()
                )
            }
        }
    }
}
