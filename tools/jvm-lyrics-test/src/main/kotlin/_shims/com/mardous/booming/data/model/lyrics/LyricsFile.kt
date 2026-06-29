// JVM shim for LyricsFile: a plain data holder mirroring the real class's shape, minus the
// Android Parcelable / kotlinx.serialization annotations (not needed to exercise the parsers).
// The parsers only ever read LyricsFile.format / Format.value in handles(file).
package com.mardous.booming.data.model.lyrics

class LyricsFile(
    val path: String,
    val format: Format
) {
    enum class Format(val value: String) {
        TTML("ttml"),
        LRC("lrc")
    }
}
