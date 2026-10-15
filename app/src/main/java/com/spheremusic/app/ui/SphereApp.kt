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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.spheremusic.app.data.SongOrder
import com.spheremusic.app.data.Playlist
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
    onQueueMove: (Long, Long) -> Unit,
    onSongOrder: (SongOrder) -> Unit,
    onLyricsFolder: () -> Unit,
    onCreatePlaylist: (String, Long?) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddToPlaylist: (String, Long) -> Unit,
    onRemoveFromPlaylist: (String, Long) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onSearchLyrics: (Song) -> Unit = {},
    onPasteLyrics: () -> String? = { null },
    onSaveCopiedLyrics: (String) -> Unit = {},
    onCancelLyricsImport: () -> Unit = {},
    onAddManyToPlaylist: (String, List<Long>) -> Unit = { _, _ -> },
    onCreateManyPlaylist: (String, List<Long>) -> Unit = { _, _ -> },
    onDeleteSongs: (List<Song>) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onShare: (Song) -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedAlbum by rememberSaveable { mutableStateOf<Long?>(null) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var nameDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var playlistMenu by remember { mutableStateOf<Playlist?>(null) }
    var deletePlaylist by remember { mutableStateOf<Playlist?>(null) }
    var actionsSong by remember { mutableStateOf<Song?>(null) }
    var addSong by remember { mutableStateOf<Song?>(null) }
    var createWithSong by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteSong by remember { mutableStateOf<Song?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var batchPicker by remember { mutableStateOf(false) }
    var batchCreate by remember { mutableStateOf(false) }
    var batchDelete by remember { mutableStateOf(false) }
    val selectedSongs = state.songs.filter { it.id in selectedIds }
    LaunchedEffect(state.songs) { selectedIds = selectedIds.filter { id -> state.songs.any { it.id == id } }.toLongArray() }
    fun selectSong(song: Song) {
        selectedIds = if (song.id in selectedIds) selectedIds.filterNot { it == song.id }.toLongArray() else selectedIds + song.id
    }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(notices) { notices.collect { snackbar.showSnackbar(context.getString(it)) } }
    val playlist = state.playlists.find { it.id == selectedPlaylist }
    LaunchedEffect(state.playlists, selectedPlaylist) {
        if (selectedPlaylist != null && playlist == null) selectedPlaylist = null
    }
    val collection = remember(state.songs, state.favorites, state.songOrder, section, selectedAlbum, playlist) {
        if (section == 3) {
            val byId = state.songs.associateBy { it.id }
            playlist?.songIds?.mapNotNull(byId::get).orEmpty()
        } else state.songOrder.sorted(state.songs.filter {
            (section != 2 || it.id in state.favorites) &&
                (section != 1 || selectedAlbum == null || it.albumId == selectedAlbum)
        })
    }
    val filtered = remember(collection, search) {
        collection.filter {
            search.isBlank() || listOf(it.title, it.artist, it.album).any { value -> value.contains(search.trim(), ignoreCase = true) }
        }
    }
    Scaffold(
        containerColor = LocalArtworkBackground.current,
        snackbarHost = { if (!playerOpen) SnackbarHost(snackbar) },
        bottomBar = {
            if (permission) {
                Column {
                    state.current?.let { song ->
                        MiniPlayer(song, state.playing, state.positionMs, state.durationMs, { playerOpen = true }, onToggle, onNext)
                    }
                    NavigationBar(containerColor = LocalArtworkBackground.current) {
                        val labels = listOf(R.string.library, R.string.albums, R.string.favorites, R.string.playlists)
                        val icons = listOf(Icons.Rounded.LibraryMusic, Icons.Rounded.Album, Icons.Rounded.FavoriteBorder, Icons.Rounded.PlaylistPlay)
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = section == index,
                                onClick = { section = index; selectedAlbum = null; selectedPlaylist = null; search = "" },
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
                    Text(stringResource(R.string.app_name), fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
                    Text(stringResource(R.string.tagline), color = SphereMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (permission) IconButton(onLyricsFolder) {
                    Icon(Icons.Rounded.FolderOpen, stringResource(R.string.lyrics_folder), tint = SpherePurple)
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
                        if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear_search)) }
                    },
                )
                Spacer(Modifier.height(16.dp))
                if (selectedIds.isNotEmpty()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ selectedIds = longArrayOf() }) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) }
                    Text(selectedSongs.size.toString(), Modifier.weight(1f))
                    IconButton({ selectedIds = filtered.map { it.id }.toLongArray() }) {
                        Icon(Icons.Rounded.DoneAll, stringResource(R.string.select_all))
                    }
                    IconButton({ batchPicker = true }) { Icon(Icons.Rounded.PlaylistAdd, stringResource(R.string.playlist_add)) }
                    IconButton({ batchDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete_song)) }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if ((section == 1 && selectedAlbum != null) || (section == 3 && playlist != null)) IconButton({
                        selectedAlbum = null; selectedPlaylist = null
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                    Text(
                        when {
                            selectedAlbum != null -> filtered.firstOrNull()?.album ?: stringResource(R.string.albums)
                            section == 1 -> stringResource(R.string.albums)
                            section == 2 -> stringResource(R.string.on_repeat)
                            section == 3 -> playlist?.name ?: stringResource(R.string.playlists)
                            else -> stringResource(R.string.songs)
                        },
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (section == 0 || (section == 3 && playlist != null)) IconButton(
                        onClick = { filtered.randomOrNull()?.let { onPlay(filtered, it, true); playerOpen = true } },
                        enabled = state.ready && filtered.isNotEmpty(),
                    ) {
                        Icon(Icons.Rounded.Shuffle, stringResource(R.string.shuffle_all), tint = SpherePurple)
                    }
                    Text(filtered.size.toString(), color = SphereMuted, style = MaterialTheme.typography.labelMedium)
                    if (section == 3) IconButton({
                        renameTarget = null; createWithSong = null; nameDialog = true
                    }) { Icon(Icons.Rounded.Add, stringResource(R.string.playlist_create)) }
                    if (section != 3) Box {
                        IconButton({ sortOpen = true }) { Icon(Icons.Rounded.Sort, stringResource(R.string.sort)) }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            SongOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (order == SongOrder.RECENT) R.string.sort_recent else R.string.sort_title)) },
                                    onClick = { onSongOrder(order); sortOpen = false },
                                    trailingIcon = {
                                        if (order == state.songOrder) Text("✓", color = SpherePurple)
                                    },
                                )
                            }
                        }
                    }
                    if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                when {
                    section == 3 && playlist == null -> {
                        if (state.playlists.isEmpty()) {
                            EmptyState(R.string.empty_playlists, R.string.empty_playlists_body, Modifier.weight(1f)) {
                                Button({ renameTarget = null; createWithSong = null; nameDialog = true }) {
                                    Text(stringResource(R.string.playlist_create))
                                }
                            }
                        } else LazyColumn(Modifier.weight(1f)) {
                            items(state.playlists.filter { search.isBlank() || it.name.contains(search.trim(), true) }, key = { it.id }) { item ->
                                Row(Modifier.fillMaxWidth().clickable { selectedPlaylist = item.id }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.PlaylistPlay, null, tint = SpherePurple)
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(item.name, fontWeight = FontWeight.Bold)
                                        Text(item.songIds.count { id -> state.songs.any { it.id == id } }.toString(), color = SphereMuted)
                                    }
                                    IconButton({ playlistMenu = item }) {
                                        Icon(Icons.Rounded.MoreVert, stringResource(R.string.playlist_actions, item.name))
                                    }
                                }
                            }
                        }
                    }
                    state.loading && state.songs.isEmpty() -> EmptyState(R.string.loading, null, Modifier.weight(1f))
                    filtered.isEmpty() -> EmptyState(
                        when {
                            search.isNotBlank() -> R.string.no_results
                            section == 2 -> R.string.empty_favorites
                            section == 3 -> R.string.empty_playlist
                            else -> R.string.empty_title
                        },
                        when {
                            search.isNotBlank() -> R.string.no_results_body
                            section == 2 -> R.string.empty_favorites_body
                            section == 3 -> R.string.empty_playlist_body
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
                                onPlay = {
                                    if (selectedIds.isNotEmpty()) selectSong(song)
                                    else if (state.ready) { onPlay(collection, song, state.shuffle); playerOpen = true }
                                },
                                onFavorite = { onFavorite(song) },
                                onMore = { actionsSong = song },
                                playing = state.playing && song.id == state.current?.id,
                                batchSelected = if (selectedIds.isNotEmpty()) song.id in selectedIds else null,
                                onLongPress = { selectSong(song) },
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
            onQueueMove, onLyricsFolder, onSongActions = { actionsSong = it }, onSearchLyrics = onSearchLyrics,
        )
    }
    if (nameDialog) PlaylistNameDialog(renameTarget, { nameDialog = false; batchCreate = false }) { name ->
        val target = renameTarget
        if (batchCreate) { onCreateManyPlaylist(name, selectedSongs.map { it.id }); selectedIds = longArrayOf() }
        else if (target == null) onCreatePlaylist(name, createWithSong) else onRenamePlaylist(target.id, name)
        nameDialog = false; createWithSong = null; batchCreate = false
    }
    addSong?.let { song ->
        PlaylistPicker(state.playlists, { addSong = null }, {
            createWithSong = song.id; renameTarget = null; addSong = null; nameDialog = true
        }) { id -> onAddToPlaylist(id, song.id); addSong = null }
    }
    actionsSong?.let { song ->
        SongActionsDialog(song, section == 3 && playlist?.songIds?.contains(song.id) == true,
            { actionsSong = null },
            { addSong = song; actionsSong = null },
            { playlist?.let { onRemoveFromPlaylist(it.id, song.id) }; actionsSong = null },
            { deleteSong = song; actionsSong = null },
            { onLinkLyrics(song); actionsSong = null },
            { onSearchLyrics(song); actionsSong = null },
            { onPlayNext(song); actionsSong = null },
            { onShare(song); actionsSong = null },
            { selectSong(song); actionsSong = null; playerOpen = false },
        )
    }
    playlistMenu?.let { item ->
        AlertDialog(onDismissRequest = { playlistMenu = null }, title = { Text(item.name) },
            text = {
                Column {
                    TextButton({ renameTarget = item; nameDialog = true; playlistMenu = null }) { Text(stringResource(R.string.playlist_rename)) }
                    TextButton({ deletePlaylist = item; playlistMenu = null }) { Text(stringResource(R.string.playlist_delete)) }
                }
            }, confirmButton = { TextButton({ playlistMenu = null }) { Text(stringResource(R.string.close)) } })
    }
    deletePlaylist?.let { item ->
        AlertDialog(onDismissRequest = { deletePlaylist = null }, title = { Text(stringResource(R.string.playlist_delete)) },
            text = { Text(stringResource(R.string.playlist_delete_body, item.name)) },
            confirmButton = { TextButton({ onDeletePlaylist(item.id); deletePlaylist = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton({ deletePlaylist = null }) { Text(stringResource(R.string.cancel)) } })
    }
    deleteSong?.let { song ->
        AlertDialog(onDismissRequest = { deleteSong = null }, title = { Text(stringResource(R.string.delete_song)) },
            text = { Text(stringResource(R.string.delete_song_body, song.title)) },
            confirmButton = { TextButton({ onDeleteSong(song); deleteSong = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton({ deleteSong = null }) { Text(stringResource(R.string.cancel)) } })
    }
    state.lyricsImport?.let { target ->
        CopiedLyricsDialog(target, state.lyricsSaving, onCancelLyricsImport, onPasteLyrics, onSaveCopiedLyrics)
    }
    if (batchPicker) PlaylistPicker(state.playlists, { batchPicker = false }, {
        batchPicker = false; batchCreate = true; renameTarget = null; nameDialog = true
    }) { id ->
        onAddManyToPlaylist(id, selectedSongs.map { it.id }); selectedIds = longArrayOf(); batchPicker = false
    }
    if (batchDelete) AlertDialog(
        onDismissRequest = { batchDelete = false },
        title = { Text(stringResource(R.string.delete_song)) },
        text = { Text(stringResource(R.string.delete_many_confirm, selectedSongs.size)) },
        confirmButton = { TextButton({
            onDeleteSongs(selectedSongs); batchDelete = false; selectedIds = longArrayOf()
        }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ batchDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
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
