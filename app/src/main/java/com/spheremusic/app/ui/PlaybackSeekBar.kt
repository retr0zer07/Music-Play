package com.spheremusic.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSeekBar(
    value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>, enabled: Boolean, modifier: Modifier = Modifier,
    playing: Boolean,
) {
    var dragging by remember(enabled, valueRange) { mutableStateOf(false) }
    val smooth by animateFloatAsState(value, tween(if (dragging || !playing) 0 else 80), label = "Playback progress")
    Slider(
        value = if (dragging) value else smooth,
        onValueChange = { dragging = true; onValueChange(it) },
        onValueChangeFinished = { dragging = false; onValueChangeFinished() },
        valueRange = valueRange, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        thumb = {
            Canvas(Modifier.size(28.dp)) {
                if (dragging) drawCircle(SphereMint.copy(alpha = 0.18f))
                drawCircle(if (enabled) SphereMint else SphereMuted, if (dragging) 10.dp.toPx() else 8.dp.toPx())
                drawCircle(SphereBackground.copy(alpha = 0.3f), 4.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
        },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val fraction = ((slider.value - valueRange.start) /
                    (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)).coerceIn(0f, 1f)
                val end = size.width * fraction
                val center = size.height / 2
                val thickness = if (dragging) 8.dp.toPx() else 6.dp.toPx()
                drawLine(SphereMuted.copy(alpha = 0.28f), Offset(0f, center), Offset(size.width, center), thickness, StrokeCap.Round)
                if (end > 0f) {
                    drawLine(
                        Brush.horizontalGradient(if (enabled) listOf(SpherePurple, SphereMint) else listOf(SphereMuted, SphereMuted)),
                        Offset(0f, center), Offset(end, center), thickness, StrokeCap.Round,
                    )
                }
            }
        },
    )
}
