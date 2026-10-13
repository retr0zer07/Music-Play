package com.spheremusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.Player
import com.spheremusic.app.MusicState
import com.spheremusic.app.R
import com.spheremusic.app.data.Song

@Composable
fun PlayerScreen(
    state: MusicState,
    snackbar: SnackbarHostState,
    onClose: () -> Unit,
    onFavorite: (Song) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueuePlay: (Int) -> Unit,
    onLinkLyrics: (Song) -> Unit,
) {
    val song = state.current ?: return
    var tab by rememberSaveable { mutableStateOf(0) }
    var dragPosition by remember(song.id) { mutableStateOf<Float?>(null) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = checkNotNull(view.parent as? DialogWindowProvider).window
            window.hideStatusBar()
            val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) window.hideStatusBar()
            }
            view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            onDispose {
                if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
        }
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        Scaffold(
            containerColor = SphereBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF292038), SphereBackground, SphereBackground)))
                    .padding(padding).padding(safeInsets).padding(horizontal = 24.dp),
            ) {
                val viewportHeight = maxHeight
                val contentHeight = (maxHeight * 0.45f).coerceAtLeast(160.dp)
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = viewportHeight),
                ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClose) { Icon(Icons.Rounded.ExpandMore, stringResource(R.string.close_player)) }
                    Text(
                        stringResource(R.string.now_playing), Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = SphereMuted,
                    )
                    FavoriteButton(song.id in state.favorites) { onFavorite(song) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    listOf(R.string.cover, R.string.lyrics, R.string.queue_short).forEachIndexed { index, label ->
                        FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(stringResource(label), style = MaterialTheme.typography.labelMedium) })
                    }
                }
                when (tab) {
                    0 -> BoxWithConstraints(Modifier.height(contentHeight).fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Artwork(song, Modifier.size(minOf(maxWidth, maxHeight)))
                    }
                    1 -> LyricsPanel(state, Modifier.height(contentHeight), onSeek) { onLinkLyrics(song) }
                    else -> {
                        Column(Modifier.height(contentHeight)) {
                            Text(pluralStringResource(R.plurals.queue_count, state.queue.size, state.queue.size), Modifier.padding(vertical = 12.dp), color = SphereMuted)
                            LazyColumn(Modifier.weight(1f)) {
                                itemsIndexed(state.queue, key = { _, item -> item.id }) { index, item ->
                                    SongRow(
                                        item, item.id in state.favorites, item.id == song.id,
                                        { onQueuePlay(index) }, { onFavorite(item) },
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                Text(song.title, Modifier.fillMaxWidth(), fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, Modifier.padding(top = 7.dp, bottom = 14.dp), color = SphereMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val seekDescription = stringResource(R.string.seek)
                Slider(
                    value = dragPosition ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = { dragPosition?.let { onSeek(it.toLong()) }; dragPosition = null },
                    valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                    enabled = state.durationMs > 0,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekDescription },
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(dragPosition?.toLong() ?: state.positionMs), color = SphereMuted, style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(state.durationMs), color = SphereMuted, style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(onShuffle) {
                        Icon(Icons.Rounded.Shuffle, stringResource(if (state.shuffle) R.string.shuffle_on else R.string.shuffle_off), tint = if (state.shuffle) SphereMint else SphereMuted)
                    }
                    IconButton(onPrevious, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), Modifier.size(34.dp)) }
                    IconButton(onToggle, Modifier.size(72.dp).clip(CircleShape).background(SphereMint)) {
                        Icon(
                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            stringResource(if (state.playing) R.string.pause else R.string.play),
                            Modifier.size(38.dp), tint = SphereBackground,
                        )
                    }
                    IconButton(onNext, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.next), Modifier.size(34.dp)) }
                    val repeatLabel = when (state.repeat) {
                        Player.REPEAT_MODE_ONE -> R.string.repeat_one
                        Player.REPEAT_MODE_ALL -> R.string.repeat_all
                        else -> R.string.repeat_off
                    }
                    IconButton(onRepeat) {
                        Icon(
                            if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            stringResource(repeatLabel), tint = if (state.repeat != Player.REPEAT_MODE_OFF) SphereMint else SphereMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPanel(state: MusicState, modifier: Modifier, onSeek: (Long) -> Unit, onLink: () -> Unit) {
    val lyrics = state.lyrics
    val listState = rememberLazyListState()
    val active = lyrics?.activeIndex(state.positionMs) ?: -1
    LaunchedEffect(state.current?.id, active) {
        if (active >= 0 && !listState.isScrollInProgress) listState.animateScrollToItem((active - 1).coerceAtLeast(0))
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.lyricsLoading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (lyrics == null) {
            EmptyState(
                R.string.no_lyrics, R.string.no_lyrics_body,
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
            )
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = listState,
                contentPadding = PaddingValues(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    Text(
                        line.text.ifBlank { "♪" },
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = lyrics.synchronized) { onSeek(line.timeMs) }.padding(vertical = 4.dp),
                        fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                        color = if (index == active || !lyrics.synchronized) MaterialTheme.colorScheme.onSurface else SphereMuted.copy(alpha = 0.65f),
                    )
                }
            }
        }
        OutlinedButton(onLink, Modifier.padding(vertical = 8.dp)) {
            Text(stringResource(R.string.link_lyrics), style = MaterialTheme.typography.labelMedium)
        }
    }
}
