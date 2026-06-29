package com.mardous.booming.data.local.lyrics

import com.mardous.booming.data.local.lyrics.lrc.LrcLyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the real Bones LRC sample (user-provided original + FR + ES stacked at each timestamp) to
 * confirm F1 collects two translation layers per line via the duplicate-timestamp convention.
 */
class BonesLrcFileTest {

    private val file = File("../../samples/multilingual-lyrics/Bones - Imagine Dragons.lrc")

    @Test
    fun `bones lrc stacks two translations per line`() {
        assertTrue("sample file missing: ${file.absolutePath}", file.exists())

        val lyrics = LrcLyricsParser().parse(
            file.readText(),
            trackLength = 165_000,
            ignoreBlankLines = false
        )
        checkNotNull(lyrics) { "parser returned null for the Bones LRC file" }

        val contentLines = lyrics.lines.filter { !it.isEmpty }
        contentLines.take(3).forEachIndexed { i, l ->
            println("DIAG[$i] '${l.content.content}' -> ${l.translations.map { it.content.content }}")
        }
        assertEquals(49, contentLines.size)

        contentLines.forEachIndexed { index, line ->
            assertEquals(
                "line $index ('${line.content.content}') should stack 2 translations",
                2,
                line.translations.size
            )
            line.translations.forEach {
                assertTrue(it.content.content.isNotBlank())
                // LRC has no language tagging yet (F2).
                assertEquals(null, it.lang)
            }
        }
    }
}
