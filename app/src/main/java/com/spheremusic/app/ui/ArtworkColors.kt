package com.spheremusic.app.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.spheremusic.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalArtworkBackground = staticCompositionLocalOf { SphereBackground }

internal fun dominantColor(bitmap: Bitmap): Int? {
    val bins = mutableMapOf<Int, Int>()
    for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
        val pixel = bitmap.getPixel(x, y)
        if (android.graphics.Color.alpha(pixel) < 128) continue
        val key = (((pixel shr 19) and 31) shl 10) or (((pixel shr 11) and 31) shl 5) or ((pixel shr 3) and 31)
        bins[key] = (bins[key] ?: 0) + 1
    }
    val key = bins.maxByOrNull { it.value }?.key ?: return null
    return android.graphics.Color.rgb(((key shr 10) and 31) * 8 + 4, ((key shr 5) and 31) * 8 + 4, (key and 31) * 8 + 4)
}

@Composable
fun rememberArtworkBackground(song: Song?): Color {
    val context = LocalContext.current
    val color by produceState(SphereBackground, song?.artworkUri) {
        value = SphereBackground
        val uri = song?.artworkUri ?: return@produceState
        value = withContext(Dispatchers.IO) {
            when (val result = context.imageLoader.execute(
                ImageRequest.Builder(context).data(uri).size(64).allowHardware(false).build(),
            )) {
                is SuccessResult -> {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let(::dominantColor)?.let { lerp(SphereBackground, Color(it), 0.28f) } ?: SphereBackground
                }
                is ErrorResult -> {
                    Log.d("SphereMusic", "Artwork color unavailable", result.throwable)
                    SphereBackground
                }
            }
        }
    }
    return animateColorAsState(color, tween(650), label = "Artwork background").value
}
