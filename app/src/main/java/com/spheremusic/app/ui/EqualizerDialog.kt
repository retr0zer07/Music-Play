package com.spheremusic.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spheremusic.app.R
import com.spheremusic.app.playback.EqualizerControl
import com.spheremusic.app.playback.EqualizerPreset

@Composable
fun EqualizerDialog(onClose: () -> Unit) {
    val state by EqualizerControl.state.collectAsStateWithLifecycle()
    var presets by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.equalizer)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.equalizer), Modifier.weight(1f))
                    Switch(state.enabled, EqualizerControl::setEnabled, enabled = state.frequencies.isNotEmpty() && !state.error)
                }
                when {
                    state.error -> Text(stringResource(R.string.equalizer_error))
                    state.frequencies.isEmpty() -> Text(stringResource(R.string.equalizer_start_playback))
                    else -> {
                        TextButton({ presets = true }) { Text(state.preset) }
                        DropdownMenu(presets, { presets = false }) {
                            EqualizerPreset.entries.forEach { preset ->
                                DropdownMenuItem({ Text(preset.label) }, {
                                    EqualizerControl.setPreset(preset); presets = false
                                })
                            }
                        }
                        state.frequencies.forEachIndexed { index, frequency ->
                            val label = if (frequency >= 1000) "${frequency / 1000f} kHz" else "$frequency Hz"
                            Text("$label  ${state.levels[index] / 100f} dB")
                            Slider(
                                state.levels[index].toFloat(), { EqualizerControl.setBand(index, it.toInt()) },
                                enabled = state.enabled,
                                valueRange = state.minimum.toFloat()..state.maximum.toFloat(),
                                modifier = Modifier.semantics { contentDescription = label },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClose) { Text(stringResource(R.string.close)) } },
    )
}
