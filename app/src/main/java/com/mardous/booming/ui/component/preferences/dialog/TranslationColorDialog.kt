package com.mardous.booming.ui.component.preferences.dialog

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.lyrics.TranslationFilter
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.theme.BoomingMusicTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Lets the user pick a fixed color for a translation language via an HSV color wheel, a brightness
 * slider and a hex field. Built with the app's Compose-in-dialog pattern. The chosen color (or null
 * to clear) is delivered to [onResult] set by the host fragment.
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

    @Composable
    private fun ColorPickerContent(
        initial: Int?,
        onColorChanged: (Int?) -> Unit
    ) {
        // HSV state drives the wheel + brightness slider; hex mirrors the composed color.
        val startHsv = remember { colorToHsv(initial ?: DEFAULT_COLOR) }
        var hue by remember { mutableFloatStateOf(startHsv[0]) }
        var saturation by remember { mutableFloatStateOf(startHsv[1]) }
        var value by remember { mutableFloatStateOf(startHsv[2]) }
        var hex by remember { mutableStateOf(colorToHex(initial ?: DEFAULT_COLOR)) }

        fun publish() {
            val color = hsvToColor(hue, saturation, value)
            hex = colorToHex(color)
            onColorChanged(color)
        }

        val currentColor = hsvToColor(hue, saturation, value)

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            HueSaturationWheel(
                hue = hue,
                saturation = saturation,
                value = value,
                onChange = { newHue, newSaturation ->
                    hue = newHue
                    saturation = newSaturation
                    publish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            // Brightness slider (HSV value).
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    publish()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(currentColor))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            )

            OutlinedTextField(
                value = hex,
                onValueChange = { input ->
                    hex = input
                    val parsed = parseHex(input)
                    if (parsed != null) {
                        val hsv = colorToHsv(parsed)
                        hue = hsv[0]
                        saturation = hsv[1]
                        value = hsv[2]
                        onColorChanged(parsed)
                    } else {
                        onColorChanged(null)
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.lyrics_translation_color_hex_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    /**
     * A hue/saturation color wheel: angle around the center encodes hue, distance from the center
     * encodes saturation. The [value] (brightness) only affects the rendered wheel colors so the
     * user can preview the picked brightness. A ring marks the current selection.
     */
    @Composable
    private fun HueSaturationWheel(
        hue: Float,
        saturation: Float,
        value: Float,
        onChange: (hue: Float, saturation: Float) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val indicatorColor = MaterialTheme.colorScheme.onSurface

        fun handlePosition(position: Offset, size: androidx.compose.ui.geometry.Size) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dx = position.x - center.x
            val dy = position.y - center.y
            val distance = hypot(dx, dy)
            // atan2 gives -180..180; normalize to 0..360 for the hue.
            var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
            if (angle < 0f) angle += 360f
            val sat = (distance / radius).coerceIn(0f, 1f)
            onChange(angle, sat)
        }

        Canvas(
            modifier = modifier
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        handlePosition(offset, size.toSize())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        handlePosition(change.position, size.toSize())
                    }
                }
        ) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Angular hue sweep.
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = (0..360 step 60).map { deg ->
                        Color(hsvToColor(deg.toFloat() % 360f, 1f, value))
                    },
                    center = center
                ),
                radius = radius,
                center = center
            )
            // Radial white->transparent for saturation falloff toward the center.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(hsvToColor(0f, 0f, value)), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Selection indicator.
            val angleRad = Math.toRadians(hue.toDouble())
            val markerDistance = saturation.coerceIn(0f, 1f) * radius
            val markerCenter = Offset(
                x = center.x + (cos(angleRad) * markerDistance).toFloat(),
                y = center.y + (sin(angleRad) * markerDistance).toFloat()
            )
            drawCircle(
                color = indicatorColor,
                radius = 10f,
                center = markerCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
        }
    }

    companion object {
        private const val ARG_LANG = "lang"
        private const val ARG_COLOR = "color"
        private const val NO_COLOR = Int.MIN_VALUE
        private const val DEFAULT_COLOR = 0xFF2E7D32.toInt() // green

        private fun colorToHex(color: Int): String =
            String.format("#%06X", 0xFFFFFF and color)

        private fun parseHex(input: String): Int? {
            val cleaned = input.trim().removePrefix("#")
            if (cleaned.length != 6) return null
            val rgb = cleaned.toLongOrNull(16) ?: return null
            return (0xFF000000.toInt()) or rgb.toInt()
        }

        private fun colorToHsv(color: Int): FloatArray {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(color, hsv)
            return hsv
        }

        private fun hsvToColor(hue: Float, saturation: Float, value: Float): Int {
            return AndroidColor.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), saturation, value))
        }

        fun newInstance(lang: String, color: Int?): TranslationColorDialog =
            TranslationColorDialog().withArgs {
                putString(ARG_LANG, lang)
                putInt(ARG_COLOR, color ?: NO_COLOR)
            }
    }
}
