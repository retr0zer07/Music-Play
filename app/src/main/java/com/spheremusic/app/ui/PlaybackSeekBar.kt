package com.spheremusic.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSeekBar(
    value: Float, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>, enabled: Boolean, modifier: Modifier = Modifier,
    playing: Boolean,
) {
    var dragging by remember { mutableStateOf(false) }
    val smooth by animateFloatAsState(value, tween(if (dragging || !playing) 0 else 80), label = "Playback progress")
    Slider(
        value = if (dragging) value else smooth,
        onValueChange = { dragging = true; onValueChange(it) },
        onValueChangeFinished = { dragging = false; onValueChangeFinished() },
        valueRange = valueRange, enabled = enabled, modifier = modifier,
        thumb = { Box(Modifier.size(if (dragging) 20.dp else 14.dp).background(SphereMint, CircleShape)) },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val fraction = ((slider.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                val end = size.width * fraction
                val center = size.height / 2
                drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, center), Offset(size.width, center), 4.dp.toPx(), StrokeCap.Round)
                val path = Path()
                val wavelength = 24.dp.toPx()
                val amplitude = if (playing && !dragging) 2.5.dp.toPx() else 0f
                path.moveTo(0f, center)
                var x = 0f
                while (x <= end) {
                    path.lineTo(x, center + sin((x / wavelength * Math.PI * 2).toFloat()) * amplitude)
                    x += 1.dp.toPx()
                }
                path.lineTo(end, center + sin((end / wavelength * Math.PI * 2).toFloat()) * amplitude)
                drawPath(path, SphereMint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        },
    )
}
