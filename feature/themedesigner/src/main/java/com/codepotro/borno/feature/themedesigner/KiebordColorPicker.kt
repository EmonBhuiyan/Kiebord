package com.codepotro.borno.feature.themedesigner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codepotro.borno.theme.ThemeColorUtils
import com.codepotro.borno.theme.designer.ThemeDesignerColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure Kotlin RGB <-> HSV conversion utilities (safe for JVM unit tests without Android runtime).
 */
internal object ColorHsvUtils {

    fun colorToHsv(color: Color): FloatArray {
        val r = color.red.coerceIn(0f, 1f)
        val g = color.green.coerceIn(0f, 1f)
        val b = color.blue.coerceIn(0f, 1f)

        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        val delta = maxVal - minVal

        var h = when {
            delta == 0f -> 0f
            maxVal == r -> 60f * (((g - b) / delta) % 6f)
            maxVal == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (h < 0f) h += 360f

        val s = if (maxVal == 0f) 0f else delta / maxVal
        val v = maxVal
        return floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    }

    fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
        val hue = ((h % 360f) + 360f) % 360f
        val sat = s.coerceIn(0f, 1f)
        val value = v.coerceIn(0f, 1f)
        val a = alpha.coerceIn(0f, 1f)

        val c = value * sat
        val x = c * (1f - kotlin.math.abs(((hue / 60f) % 2f) - 1f))
        val m = value - c

        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = a
        )
    }
}

private val CuratedColorSwatches = listOf(
    // Dark & Slate Neutrals
    Color(0xFF000000), Color(0xFF121316), Color(0xFF1B1B1F), Color(0xFF212328),
    Color(0xFF2C2C30), Color(0xFF38383F), Color(0xFF424242), Color(0xFF6B7280),
    // Light & Crisp Neutrals
    Color(0xFFFFFFFF), Color(0xFFF4F6F9), Color(0xFFEBEEF2), Color(0xFFDCE0E6),
    Color(0xFFE3E2E6), Color(0xFF9EABC0), Color(0xFF1C1C1E), Color(0xFF04294E),
    // Vibrant Accents & Borno Classics
    Color(0xFF4999FD), Color(0xFF2563EB), Color(0xFF0284C7), Color(0xFF06B6D4),
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E),
    Color(0xFFEF4444), Color(0xFFFF5722), Color(0xFFF59E0B), Color(0xFF10B981),
    Color(0xFF14B8A6), Color(0xFFA8C7FA), Color(0xFF38BDF8), Color(0xFFFDFE03)
)

internal fun formatColorHex(color: Color, allowAlpha: Boolean): String {
    val a = (color.alpha * 255f).roundToInt().coerceIn(0, 255)
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    return if (!allowAlpha || a >= 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun KiebordColorPickerSheet(
    title: String,
    initialColor: Color,
    allowAlpha: Boolean = true,
    colors: ThemeDesignerColors,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current

    val initialHsv = remember(initialColor) { ColorHsvUtils.colorToHsv(initialColor) }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initialColor) { mutableFloatStateOf(if (allowAlpha) initialColor.alpha.coerceIn(0f, 1f) else 1f) }

    val currentColor = remember(hue, saturation, value, alpha, allowAlpha) {
        ColorHsvUtils.hsvToColor(hue, saturation, value, if (allowAlpha) alpha else 1f)
    }

    var hexText by remember(initialColor) {
        mutableStateOf(formatColorHex(initialColor, allowAlpha))
    }
    var copiedFeedback by remember { mutableStateOf(false) }
    var pastedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(1500)
            copiedFeedback = false
        }
    }
    LaunchedEffect(pastedFeedback) {
        if (pastedFeedback) {
            delay(1500)
            pastedFeedback = false
        }
    }

    fun syncHexFromHsv(h: Float, s: Float, v: Float, a: Float) {
        val color = ColorHsvUtils.hsvToColor(h, s, v, if (allowAlpha) a else 1f)
        hexText = formatColorHex(color, allowAlpha)
    }

    fun applyPickedColor(picked: Color) {
        val hsv = ColorHsvUtils.colorToHsv(picked)
        val newAlpha = if (allowAlpha) picked.alpha.coerceIn(0f, 1f) else 1f
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        alpha = newAlpha
        hexText = formatColorHex(picked.copy(alpha = newAlpha), allowAlpha)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithFadingEdge(rememberScrollState(), topEdgeHeight = 16.dp, bottomEdgeHeight = 20.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current vs New comparison row + Hex input + Quick Copy/Paste
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(initialColor)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Old", fontSize = 11.sp, color = colors.textSecondary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                            .border(1.5.dp, colors.accent, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("New", fontSize = 11.sp, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val cleaned = input.trim().removePrefix("#")
                        if (cleaned.length == 3 || cleaned.length == 4 || cleaned.length == 6 || cleaned.length == 8) {
                            val parsed = ThemeColorUtils.parseHexColor("#$cleaned", Color.Unspecified)
                            if (parsed != Color.Unspecified) {
                                val hsv = ColorHsvUtils.colorToHsv(parsed)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                if (allowAlpha) {
                                    alpha = if (cleaned.length == 4 || cleaned.length == 8) parsed.alpha else 1f
                                }
                            }
                        }
                    },
                    label = { Text("HEX", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.inputBorder,
                        focusedContainerColor = colors.inputBackground,
                        unfocusedContainerColor = colors.inputBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Copy & Paste buttons for the chosen color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val hexToCopy = formatColorHex(currentColor, allowAlpha)
                            clipboardManager.setText(AnnotatedString(hexToCopy))
                            copiedFeedback = true
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (copiedFeedback) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy color",
                        tint = if (copiedFeedback) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copiedFeedback) "Copied" else "Copy Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (copiedFeedback) colors.accent else colors.textPrimary
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                            val normalized = clipText.removePrefix("#")
                            if (normalized.length in setOf(3, 4, 6, 8)) {
                                val parsed = ThemeColorUtils.parseHexColor("#$normalized", Color.Unspecified)
                                if (parsed != Color.Unspecified) {
                                    applyPickedColor(parsed)
                                    pastedFeedback = true
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (pastedFeedback) Icons.Outlined.Check else Icons.Outlined.ContentPaste,
                        contentDescription = "Paste color",
                        tint = if (pastedFeedback) colors.accent else colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pastedFeedback) "Pasted" else "Paste Color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pastedFeedback) colors.accent else colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive HSV Color Wheel
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HsvColorWheel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    borderColor = colors.cardBorder,
                    onHueSaturationChanged = { newHue, newSat ->
                        hue = newHue
                        saturation = newSat
                        syncHexFromHsv(newHue, newSat, value, alpha)
                    },
                    modifier = Modifier.size(204.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Brightness / Value Slider with dynamic gradient track
            Text(
                text = "Brightness (${(value * 100).roundToInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val fullBrightColor = remember(hue, saturation) {
                    ColorHsvUtils.hsvToColor(hue, saturation, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(listOf(Color.Black, fullBrightColor)))
                        .border(0.5.dp, colors.cardBorder, RoundedCornerShape(5.dp))
                )
                Slider(
                    value = value,
                    onValueChange = { newVal ->
                        value = newVal
                        syncHexFromHsv(hue, saturation, newVal, alpha)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textPrimary,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            if (allowAlpha) {
                Text(
                    text = "Opacity (${(alpha * 100).roundToInt()}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Slider(
                    value = alpha,
                    onValueChange = { newAlpha ->
                        alpha = newAlpha
                        syncHexFromHsv(hue, saturation, value, newAlpha)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quick Swatches",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CuratedColorSwatches.forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, colors.cardBorder, CircleShape)
                            .clickable { applyPickedColor(swatch) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two stacked filled buttons matching Kiebord's bottom sheet convention
            Button(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Apply Color",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color(0xFF0F141C) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) Color(0xFF2C2C32) else Color(0xFFE0E2EC),
                    contentColor = colors.textPrimary
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

private val HueWheelColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000)
)

@Composable
private fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    borderColor: Color,
    onHueSaturationChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun updateFromOffset(pos: Offset) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = (min(size.width, size.height) / 2f).coerceAtLeast(1f)
                    val dx = pos.x - centerX
                    val dy = pos.y - centerY
                    val dist = hypot(dx, dy)
                    val newSat = (dist / radius).coerceIn(0f, 1f)
                    var angleDeg = (atan2(dy, dx) * (180.0 / PI)).toFloat()
                    if (angleDeg < 0f) angleDeg += 360f
                    onHueSaturationChanged(angleDeg, newSat)
                }

                updateFromOffset(down.position)
                down.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    updateFromOffset(change.position)
                    change.consume()
                }
            }
        }
    ) {
        val radius = min(size.width, size.height) / 2f
        val wheelCenter = center

        drawCircle(
            brush = Brush.sweepGradient(HueWheelColors, center = wheelCenter),
            radius = radius,
            center = wheelCenter
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = wheelCenter,
                radius = radius
            ),
            radius = radius,
            center = wheelCenter
        )

        if (value < 1f) {
            drawCircle(
                color = Color.Black.copy(alpha = (1f - value).coerceIn(0f, 1f)),
                radius = radius,
                center = wheelCenter
            )
        }

        drawCircle(
            color = borderColor,
            radius = radius,
            center = wheelCenter,
            style = Stroke(width = 1.dp.toPx())
        )

        val angleRad = hue * (PI / 180.0)
        val thumbDist = saturation.coerceIn(0f, 1f) * radius
        val thumbOffset = Offset(
            x = wheelCenter.x + (cos(angleRad) * thumbDist).toFloat(),
            y = wheelCenter.y + (sin(angleRad) * thumbDist).toFloat()
        )
        val thumbRadius = 10.dp.toPx()
        val selectedWheelColor = ColorHsvUtils.hsvToColor(hue, saturation, value)

        drawCircle(
            color = selectedWheelColor,
            radius = thumbRadius,
            center = thumbOffset
        )
        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = thumbOffset,
            style = Stroke(width = 2.5.dp.toPx())
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = thumbRadius + 1.2.dp.toPx(),
            center = thumbOffset,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}


