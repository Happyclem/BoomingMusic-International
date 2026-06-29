// JVM shim providing ONLY the two String extensions the TTML parser depends on.
// Copied verbatim from app/src/main/java/com/mardous/booming/extensions/utilities/Utilities.kt
// (the real file additionally imports android.os.Build / kotlinx.serialization, which we don't
// want to pull into this standalone JVM project). Keep these in sync if the originals change.
package com.mardous.booming.extensions.utilities

private val SPACES_REGEX = Regex("\\s+")

fun String?.isWhitespace() = this != null && this.length == 1 && this[0] == ' '

fun String.collapseSpaces() = trim().replace(SPACES_REGEX, " ")
