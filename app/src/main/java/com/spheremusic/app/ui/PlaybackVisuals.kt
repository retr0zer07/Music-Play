package com.spheremusic.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spheremusic.app.R
import com.spheremusic.app.data.Song
import kotlin.coroutines.coroutineContext
import kotlin.math.sin
import kotlinx.coroutines.isActive

internal val PlaybackPhaseKey = SemanticsPropertyKey<Float>("PlaybackAnimationPhase")
internal var SemanticsPropertyReceiver.playbackPhase by PlaybackPhaseKey

@Composable
private fun playbackPhase(playing: Boolean, identity: Long, periodMs: Float): Float {
    var phase by remember(identity) { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(playing, identity, lifecycle) {
        if (!playing) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous = withFrameNanos { it }
            while (isActive) {
                val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
                withFrameNanos { now ->
                    if (scale > 0f) {
                        val elapsedMs = ((now - previous) / 1_000_000f).coerceIn(0f, 100f)
                        phase = (phase + elapsedMs / (periodMs * scale)) % 1f
                    }
                    previous = now
                }
            }
        }
    }
    return phase
}

@Composable
fun PlaybackEqualizer(playing: Boolean, song: Song, modifier: Modifier = Modifier) {
    val phase = playbackPhase(playing, song.id, 1_100f)
    val description = stringResource(if (playing) R.string.song_playing else R.string.song_paused, song.title)
    Canvas(modifier.semantics {
        contentDescription = description
        playbackPhase = phase
    }) {
        val barWidth = size.width / 7f
        repeat(4) { bar ->
            val fraction = if (playing) {
                0.3f + 0.7f * (0.5f + 0.5f * sin((phase * 2 * Math.PI + bar * 1.7).toFloat()))
            } else 0.22f + bar % 2 * 0.12f
            val height = size.height * fraction
            drawRoundRect(
                if (playing) SphereMint else SpherePurple,
                Offset(bar * barWidth * 2, size.height - height),
                Size(barWidth, height), CornerRadius(barWidth / 2),
            )
        }
    }
}

@Composable
fun RecordArtwork(song: Song, playing: Boolean, modifier: Modifier = Modifier) {
    val phase = playbackPhase(playing, song.id, 12_000f)
    val description = stringResource(R.string.record_description, song.title)
    val status = stringResource(if (playing) R.string.record_spinning else R.string.record_paused)
    Box(
        modifier.semantics {
            contentDescription = description
            stateDescription = status
            playbackPhase = phase
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { rotationZ = phase * 360f }
                .clip(CircleShape).background(Color(0xFF111017)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0xFF37333F), Color(0xFF0C0B10))), radius)
                for (ring in 1..12) {
                    drawCircle(Color.White.copy(alpha = 0.09f), radius * (0.64f + ring * 0.025f), style = Stroke(1.dp.toPx()))
                }
                drawArc(Color.White.copy(alpha = 0.2f), -65f, 52f, false,
                    Offset(radius * 0.08f, radius * 0.08f), Size(radius * 1.84f, radius * 1.84f),
                    style = Stroke(radius * 0.035f))
                drawCircle(SpherePurple.copy(alpha = 0.55f), radius * 0.97f, style = Stroke(1.dp.toPx()))
            }
            Artwork(song, Modifier.fillMaxSize(0.64f).clip(CircleShape))
            Box(Modifier.fillMaxSize(0.09f).clip(CircleShape).background(SphereBackground))
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.5f), size.minDimension * 0.046f, style = Stroke(1.dp.toPx()))
            }
        }
    }
}
