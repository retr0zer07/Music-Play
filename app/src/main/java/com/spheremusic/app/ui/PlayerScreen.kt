package com.spheremusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import kotlinx.coroutines.launch

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
    onQueueMove: (Long, Long) -> Unit = { _, _ -> },
    onLyricsFolder: () -> Unit = {},
    onSongActions: (Song) -> Unit = {},
    onSearchLyrics: (Song) -> Unit = {},
) {
    val song = state.current ?: return
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var equalizerOpen by rememberSaveable { mutableStateOf(false) }
    var dragPosition by remember(song.id) { mutableStateOf<Float?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lyrics = state.lyrics
    val active = lyrics?.activeIndex(state.positionMs) ?: -1
    LaunchedEffect(song.id) { list.scrollToItem(0) }
    LaunchedEffect(song.id, active) {
        // Follow timed lyrics only after the listener scrolls into them, never pull them off the cover.
        if (!queueOpen && active >= 0 && list.firstVisibleItemIndex >= 1 && !list.isScrollInProgress) {
            list.animateScrollToItem(2 + (active - 1).coerceAtLeast(0))
        }
    }
    Dialog(
        onDismissRequest = { if (queueOpen) queueOpen = false else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = checkNotNull(view.parent as? DialogWindowProvider).window
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.rgb(12, 11, 18)))
            window.hideStatusBar()
            val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) window.hideStatusBar()
            }
            view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            onDispose {
                if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
        }
        Scaffold(
            containerColor = LocalArtworkBackground.current,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(LocalArtworkBackground.current, LocalArtworkBackground.current, SphereBackground)))
                    .padding(padding).padding(WindowInsets.safeDrawing.asPaddingValues()).padding(horizontal = 24.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ if (queueOpen) queueOpen = false else onClose() }) {
                        Icon(
                            if (queueOpen) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.ExpandMore,
                            stringResource(if (queueOpen) R.string.close_queue else R.string.close_player),
                        )
                    }
                    Text(
                        stringResource(if (queueOpen) R.string.queue else R.string.now_playing),
                        Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = SphereMuted,
                    )
                    FavoriteButton(song.id in state.favorites) { onFavorite(song) }
                    IconButton({ onSongActions(song) }) {
                        Icon(Icons.Rounded.MoreVert, stringResource(R.string.song_actions, song.title))
                    }
                }
                if (queueOpen) {
                    QueuePanel(
                        state, Modifier.weight(1f), { onQueuePlay(it); queueOpen = false },
                        onQueueMove, onFavorite, onSongActions,
                    )
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(song.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium)
                        IconButton(onToggle) {
                            Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (state.playing) R.string.pause else R.string.play), tint = SphereMint)
                        }
                        IconButton(onNext) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.next)) }
                    }
                } else BoxWithConstraints(Modifier.weight(1f)) {
                    val viewportHeight = maxHeight
                    val artworkHeight = (maxHeight * 0.47f).coerceIn(160.dp, 380.dp)
                    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 28.dp)) {
                        item(key = "cover") {
                            Column(Modifier.fillMaxWidth().heightIn(min = (viewportHeight - 28.dp).coerceAtLeast(0.dp))) {
                                BoxWithConstraints(
                                    Modifier.height(artworkHeight).fillMaxWidth().padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    RecordArtwork(song, state.playing, Modifier.size(minOf(maxWidth, maxHeight)))
                                }
                                Text(song.title, Modifier.fillMaxWidth(), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, Modifier.padding(top = 7.dp, bottom = 14.dp), color = SphereMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val seekDescription = stringResource(R.string.seek)
                                PlaybackSeekBar(
                                    value = dragPosition ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                                    onValueChange = { dragPosition = it },
                                    onValueChangeFinished = { dragPosition?.let { onSeek(it.toLong()) }; dragPosition = null },
                                    valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                                    enabled = state.durationMs > 0,
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekDescription },
                                    playing = state.playing,
                                )
                                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(formatTime(dragPosition?.toLong() ?: state.positionMs), color = SphereMuted,
                                        style = MaterialTheme.typography.labelSmall)
                                    Text(formatTime(state.durationMs), color = SphereMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    IconButton(onShuffle) {
                                        Icon(Icons.Rounded.Shuffle,
                                            stringResource(if (state.shuffle) R.string.shuffle_on else R.string.shuffle_off),
                                            tint = if (state.shuffle) SphereMint else SphereMuted)
                                    }
                                    IconButton(onPrevious) {
                                        Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), Modifier.size(34.dp))
                                    }
                                    IconButton(onToggle, Modifier.size(72.dp).clip(CircleShape).background(SphereMint)) {
                                        Icon(
                                            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            stringResource(if (state.playing) R.string.pause else R.string.play),
                                            Modifier.size(38.dp), tint = SphereBackground,
                                        )
                                    }
                                    IconButton(onNext) {
                                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.next), Modifier.size(34.dp))
                                    }
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
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton({ equalizerOpen = true }) {
                                        Icon(Icons.Rounded.Equalizer, stringResource(R.string.equalizer), tint = SpherePurple)
                                    }
                                    TextButton({ scope.launch { list.animateScrollToItem(1) } }, Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.ExpandMore, null)
                                        Text(stringResource(R.string.lyrics_scroll_hint), style = MaterialTheme.typography.labelMedium)
                                    }
                                    IconButton({ queueOpen = true }) {
                                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.open_queue), tint = SpherePurple)
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                            }
                        }
                        item(key = "lyrics-header") {
                            Text(
                                stringResource(R.string.lyrics),
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .background(Color(0xFF332445)).padding(20.dp).semantics { heading() },
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            )
                        }
                        when {
                            state.lyricsLoading -> item(key = "lyrics-loading") {
                                Box(Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF332445)),
                                    contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            }
                            lyrics == null -> item(key = "lyrics-setup") {
                                Column(
                                    Modifier.fillMaxWidth().background(Color(0xFF332445)).padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    EmptyState(R.string.no_lyrics, R.string.no_lyrics_body)
                                    Text(stringResource(R.string.lyrics_auto_help), color = SphereMuted,
                                        style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton({ onLinkLyrics(song) }, Modifier.padding(vertical = 8.dp)) {
                                        Text(stringResource(R.string.link_lyrics), style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(onLyricsFolder) {
                                        Text(stringResource(if (state.lyricsFolder == null) R.string.lyrics_folder else R.string.lyrics_folder_change),
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton({ onSearchLyrics(song) }, Modifier.padding(top = 8.dp)) {
                                        Text(stringResource(R.string.search_lyrics), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            else -> itemsIndexed(lyrics.lines, key = { index, _ -> "lyric-$index" }) { index, line ->
                                Text(
                                    line.text.ifBlank { "♪" },
                                    Modifier.fillMaxWidth().background(Color(0xFF332445))
                                        .clickable(enabled = lyrics.synchronized) { onSeek(line.timeMs) }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                                    color = if (index == active || !lyrics.synchronized) MaterialTheme.colorScheme.onSurface
                                        else SphereMuted.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (equalizerOpen) EqualizerDialog { equalizerOpen = false }
    }
}
