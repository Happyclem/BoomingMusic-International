package com.mardous.booming.ui.component.preferences.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.lyrics.TranslationFilter
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.theme.BoomingMusicTheme

/**
 * Lets the user pick a fixed color for a translation language via a hex field and a few preset
 * swatches. Built with the app's Compose-in-dialog pattern. The chosen color (or null to clear) is
 * delivered to [onResult] set by the host fragment.
 */
class TranslationColorDialog : DialogFragment() {

    private val lang: String get() = requireArguments().getString(ARG_LANG).orEmpty()
    private val initialColor: Int?
        get() = requireArguments().getInt(ARG_COLOR, NO_COLOR).takeIf { it != NO_COLOR }

    var onResult: ((lang: String, color: Int?) -> Unit)? = null

    private var pendingColor: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        pendingColor = initialColor
        val title = getString(
            R.string.lyrics_translation_color_for,
            TranslationFilter.displayLanguage(lang)
        )
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(createContentView(requireContext()))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onResult?.invoke(lang, pendingColor)
            }
            .setNeutralButton(R.string.lyrics_translation_color_clear) { _, _ ->
                onResult?.invoke(lang, null)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun createContentView(context: Context): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BoomingMusicTheme {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        ColorPickerContent(
                            initial = initialColor,
                            onColorChanged = { pendingColor = it }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ColorPickerContent(
        initial: Int?,
        onColorChanged: (Int?) -> Unit
    ) {
        var hex by remember { mutableStateOf(initial?.let { colorToHex(it) } ?: "") }
        var selected by remember { mutableStateOf(initial) }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PRESETS.forEach { preset ->
                    ColorSwatch(
                        color = Color(preset),
                        selected = selected == preset,
                        onClick = {
                            selected = preset
                            hex = colorToHex(preset)
                            onColorChanged(preset)
                        }
                    )
                }
            }

            OutlinedTextField(
                value = hex,
                onValueChange = { input ->
                    hex = input
                    val parsed = parseHex(input)
                    selected = parsed
                    onColorChanged(parsed)
                },
                singleLine = true,
                label = { Text(stringRes(R.string.lyrics_translation_color_hex_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
        val borderColor = MaterialTheme.colorScheme.onSurface
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(3.dp, borderColor, CircleShape) else Modifier
                )
                .clickable(onClick = onClick)
        )
    }

    @Composable
    private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)

    companion object {
        private const val ARG_LANG = "lang"
        private const val ARG_COLOR = "color"
        private const val NO_COLOR = Int.MIN_VALUE

        private val PRESETS = listOf(
            0xFF2E7D32.toInt(), // green
            0xFFC62828.toInt(), // red
            0xFF1565C0.toInt(), // blue
            0xFFF9A825.toInt(), // amber
            0xFF6A1B9A.toInt(), // purple
            0xFF00838F.toInt(), // teal
            0xFFD84315.toInt(), // deep orange
            0xFFAD1457.toInt()  // pink
        )

        private fun colorToHex(color: Int): String =
            String.format("#%06X", 0xFFFFFF and color)

        private fun parseHex(input: String): Int? {
            val cleaned = input.trim().removePrefix("#")
            if (cleaned.length != 6) return null
            val rgb = cleaned.toLongOrNull(16) ?: return null
            return (0xFF000000.toInt()) or rgb.toInt()
        }

        fun newInstance(lang: String, color: Int?): TranslationColorDialog =
            TranslationColorDialog().withArgs {
                putString(ARG_LANG, lang)
                putInt(ARG_COLOR, color ?: NO_COLOR)
            }
    }
}
