package com.spheremusic.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spheremusic.app.R
import com.spheremusic.app.data.Song
import java.util.Locale

fun formatTime(milliseconds: Long): String {
    val total = milliseconds.coerceAtLeast(0) / 1_000
    return if (total >= 3_600) {
        String.format(Locale.ROOT, "%d:%02d:%02d", total / 3_600, total / 60 % 60, total % 60)
    } else {
        String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
    }
}

@Composable
fun Artwork(song: Song?, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFF6E4BB5), Color(0xFF25133E)),
        listOf(Color(0xFF2C8C85), Color(0xFF102F41)),
        listOf(Color(0xFFBA685D), Color(0xFF44274F)),
        listOf(Color(0xFF647FC2), Color(0xFF242244)),
    )
    val palette = palettes[Math.floorMod(song?.albumId ?: 0L, palettes.size.toLong()).toInt()]
    var failed by remember(song?.artworkUri) { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(palette)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension
            for (ring in 1..4) {
                drawCircle(
                    Color.White.copy(alpha = 0.10f), radius * ring / 8,
                    Offset(size.width * 0.62f, size.height * 0.42f), style = Stroke(radius * 0.004f),
                )
            }
            drawLine(
                Color.White.copy(alpha = 0.12f),
                Offset(0f, size.height * 0.85f), Offset(size.width, size.height * 0.2f),
                strokeWidth = radius * 0.10f,
            )
        }
        Icon(Icons.Rounded.MusicNote, null, Modifier.fillMaxSize(0.34f), tint = Color.White.copy(alpha = 0.8f))
        if (song?.artworkUri != null && !failed) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = stringResource(R.string.cover_description, song.album),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { failed = true },
            )
        }
    }
}

@Composable
fun FavoriteButton(favorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            stringResource(if (favorite) R.string.favorite_remove else R.string.favorite_add),
            tint = if (favorite) SpherePurple else SphereMuted,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SongRow(
    song: Song, favorite: Boolean, selected: Boolean, onPlay: () -> Unit,
    onFavorite: () -> Unit, onMore: (() -> Unit)? = null, playing: Boolean = false,
    batchSelected: Boolean? = null, onLongPress: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (selected) SpherePurple.copy(alpha = 0.16f) else Color.Transparent)
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress)
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (batchSelected != null) Checkbox(batchSelected, { onPlay() })
        else Artwork(song, Modifier.size(54.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold, color = if (selected) SpherePurple else MaterialTheme.colorScheme.onSurface,
            )
            Text(song.artist, color = SphereMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) PlaybackEqualizer(playing, song, Modifier.size(22.dp))
        else Text(formatTime(song.durationMs), color = SphereMuted, style = MaterialTheme.typography.labelSmall)
        FavoriteButton(favorite, onFavorite)
        if (onMore != null) IconButton(onMore) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.song_actions, song.title))
        }
    }
}
