package com.spheremusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spheremusic.app.MusicState
import com.spheremusic.app.R
import com.spheremusic.app.data.Song
import kotlinx.coroutines.flow.Flow

@Composable
fun SphereApp(
    state: MusicState,
    notices: Flow<Int>,
    permission: Boolean,
    denied: Boolean,
    onPermission: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (List<Song>, Song, Boolean) -> Unit,
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
    var section by rememberSaveable { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedAlbum by rememberSaveable { mutableStateOf<Long?>(null) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(notices) { notices.collect { snackbar.showSnackbar(context.getString(it)) } }
    val filtered = remember(state.songs, state.favorites, section, search, selectedAlbum) {
        state.songs.filter {
            (section != 2 || it.id in state.favorites) &&
                (section != 1 || selectedAlbum == null || it.albumId == selectedAlbum) &&
                (search.isBlank() || listOf(it.title, it.artist, it.album).any { value -> value.contains(search.trim(), ignoreCase = true) })
        }
    }
    Scaffold(
        containerColor = SphereBackground,
        snackbarHost = { if (!playerOpen) SnackbarHost(snackbar) },
        bottomBar = {
            if (permission) {
                Column {
                    state.current?.let { song ->
                        MiniPlayer(song, state.playing, state.positionMs, state.durationMs, { playerOpen = true }, onToggle, onNext)
                    }
                    NavigationBar(containerColor = SphereBackground) {
                        val labels = listOf(R.string.library, R.string.albums, R.string.favorites)
                        val icons = listOf(Icons.Rounded.LibraryMusic, Icons.Rounded.Album, Icons.Rounded.FavoriteBorder)
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = section == index,
                                onClick = { section = index; selectedAlbum = null },
                                icon = { Icon(icons[index], null) },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.sphere_logo), null, Modifier.size(44.dp), tint = Color.Unspecified)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sphere Music", fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
                    Text(stringResource(R.string.tagline), color = SphereMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (permission) IconButton(onRefresh, enabled = !state.loading) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.refresh), tint = SphereMuted)
                }
            }
            if (!permission) {
                EmptyState(
                    R.string.permission_title,
                    if (denied) R.string.permission_denied else R.string.permission_body,
                    Modifier.weight(1f),
                ) {
                    Button(if (denied) onSettings else onPermission) {
                        Text(stringResource(if (denied) R.string.open_settings else R.string.grant_permission))
                    }
                }
            } else {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true,
                    placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.close_player)) }
                    },
                )
                Spacer(Modifier.height(16.dp))
                if (section == 0 && search.isBlank()) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF332345), Color(0xFF191D2D))))
                            .padding(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.offline_badge), color = SphereMint, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                            Text(stringResource(R.string.your_music), fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.song_count, state.songs.size), color = SphereMuted, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { filtered.randomOrNull()?.let { onPlay(filtered, it, true) } },
                                enabled = state.ready && filtered.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = SphereMint, contentColor = SphereBackground),
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.shuffle_all), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (section == 1 && selectedAlbum != null) IconButton({ selectedAlbum = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                    Text(
                        when {
                            selectedAlbum != null -> filtered.firstOrNull()?.album ?: stringResource(R.string.albums)
                            section == 1 -> stringResource(R.string.albums)
                            section == 2 -> stringResource(R.string.on_repeat)
                            else -> stringResource(R.string.songs)
                        },
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                when {
                    state.loading && state.songs.isEmpty() -> EmptyState(R.string.loading, null, Modifier.weight(1f))
                    filtered.isEmpty() -> EmptyState(
                        when {
                            search.isNotBlank() -> R.string.no_results
                            section == 2 -> R.string.empty_favorites
                            else -> R.string.empty_title
                        },
                        when {
                            search.isNotBlank() -> R.string.no_results_body
                            section == 2 -> R.string.empty_favorites_body
                            else -> R.string.empty_body
                        },
                        Modifier.weight(1f),
                    ) { if (section != 2) OutlinedButton(onRefresh) { Text(stringResource(R.string.refresh)) } }
                    section == 1 && selectedAlbum == null -> {
                        val albums = filtered.groupBy { it.albumId }.values.toList()
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(140.dp), modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(bottom = 20.dp),
                        ) {
                            items(albums, key = { it.first().albumId }) { songs ->
                                val song = songs.first()
                                Column(Modifier.clip(RoundedCornerShape(20.dp)).clickable { selectedAlbum = song.albumId }) {
                                    Artwork(song, Modifier.fillMaxWidth().height(150.dp))
                                    Text(song.album, Modifier.padding(top = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(song.artist, color = SphereMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(filtered, key = { it.id }) { song ->
                            SongRow(
                                song, song.id in state.favorites, song.id == state.current?.id,
                                onPlay = { if (state.ready) { onPlay(filtered, song, false); playerOpen = true } },
                                onFavorite = { onFavorite(song) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (playerOpen && state.current != null) {
        PlayerScreen(
            state, snackbar, { playerOpen = false }, onFavorite, onToggle, onNext,
            onPrevious, onSeek, onShuffle, onRepeat, onQueuePlay, onLinkLyrics,
        )
    }
}

@Composable
fun EmptyState(title: Int, body: Int?, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.size(60.dp), tint = SpherePurple)
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (body != null) Text(stringResource(body), color = SphereMuted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action()
    }
}

@Composable
private fun MiniPlayer(song: Song, playing: Boolean, position: Long, duration: Long, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
            .background(SphereSurface).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(song, Modifier.size(46.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SphereMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onToggle) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(if (playing) R.string.pause else R.string.play)) }
            IconButton(onNext) { Icon(Icons.Rounded.SkipNext, stringResource(R.string.next)) }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(SpherePurple.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f).height(2.dp).background(SpherePurple))
        }
    }
}
