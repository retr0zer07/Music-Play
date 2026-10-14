package com.spheremusic.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spheremusic.app.LyricsImport
import com.spheremusic.app.R
import com.spheremusic.app.data.CopiedLyrics

@Composable
fun CopiedLyricsDialog(
    target: LyricsImport, saving: Boolean, onCancel: () -> Unit,
    onPaste: () -> String?, onSave: (String) -> Unit,
) {
    var text by remember(target.songId, target.text) { mutableStateOf(target.text) }
    val tooLarge = text.length > CopiedLyrics.MAX_BYTES || text.toByteArray(Charsets.UTF_8).size > CopiedLyrics.MAX_BYTES
    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(stringResource(R.string.lyrics_import_title, target.title)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.lyrics_import_help))
                if (text.isBlank()) Text(stringResource(R.string.lyrics_clipboard_empty))
                TextButton({ onPaste()?.let { text = it } }, enabled = !saving) {
                    Text(stringResource(R.string.paste_lyrics))
                }
                OutlinedTextField(
                    text, { text = it }, enabled = !saving, minLines = 4, maxLines = 10,
                    label = { Text(stringResource(R.string.lyrics_preview)) }, isError = tooLarge,
                )
                if (tooLarge) Text(stringResource(R.string.invalid_lyrics))
            }
        },
        confirmButton = {
            TextButton({ onSave(text) }, enabled = !saving && text.isNotBlank() && !tooLarge) {
                Text(stringResource(if (saving) R.string.lyrics_saving else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onCancel, enabled = !saving) { Text(stringResource(R.string.cancel)) }
        },
    )
}
