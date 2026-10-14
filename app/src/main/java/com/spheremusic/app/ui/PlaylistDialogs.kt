package com.spheremusic.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spheremusic.app.R
import com.spheremusic.app.data.Playlist
import com.spheremusic.app.data.Song

@Composable
fun PlaylistNameDialog(playlist: Playlist?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (playlist == null) R.string.playlist_create else R.string.playlist_rename)) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.playlist_name)) }) },
        confirmButton = { TextButton({ onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PlaylistPicker(playlists: List<Playlist>, onDismiss: () -> Unit, onCreate: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_add)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    TextButton({ onSelect(playlist.id) }, Modifier.fillMaxWidth()) { Text(playlist.name) }
                }
            }
        },
        confirmButton = { TextButton(onCreate) { Text(stringResource(R.string.playlist_create)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun SongActionsDialog(
    song: Song, inPlaylist: Boolean, onDismiss: () -> Unit, onAdd: () -> Unit,
    onRemove: () -> Unit, onDelete: () -> Unit, onLinkLyrics: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(song.title) },
        text = {
            Column {
                TextButton(onAdd) { Text(stringResource(R.string.playlist_add)) }
                if (inPlaylist) TextButton(onRemove) { Text(stringResource(R.string.playlist_remove_song)) }
                TextButton(onLinkLyrics) { Text(stringResource(R.string.link_lyrics)) }
                TextButton(onDelete) { Text(stringResource(R.string.delete_song)) }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
