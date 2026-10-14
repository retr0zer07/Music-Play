package com.spheremusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spheremusic.app.MusicState
import com.spheremusic.app.R
import com.spheremusic.app.data.Song
import kotlinx.coroutines.delay

@Composable
fun QueuePanel(
    state: MusicState,
    modifier: Modifier,
    onPlay: (Int) -> Unit,
    onMove: (Long, Long) -> Unit,
    onFavorite: (Song) -> Unit,
    onSongActions: ((Song) -> Unit)? = null,
) {
    val list = rememberLazyListState()
    val queue by rememberUpdatedState(state.queue)
    val move by rememberUpdatedState(onMove)
    var dragging by remember { mutableStateOf<Long?>(null) }
    var dragY by remember { mutableStateOf(0f) }
    var lastTarget by remember { mutableStateOf<Long?>(null) }
    val up = stringResource(R.string.queue_up)
    val down = stringResource(R.string.queue_down)

    fun reorderAtPointer() {
        val source = dragging ?: return
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            dragY >= it.offset && dragY <= it.offset + it.size
        } ?: return
        val targetId = target.key as? Long ?: return
        if (targetId == source) {
            lastTarget = null
        } else if (targetId != lastTarget) {
            lastTarget = targetId
            move(source, targetId)
        }
    }

    LaunchedEffect(dragging) {
        while (dragging != null) {
            val info = list.layoutInfo
            val step = when {
                dragY < info.viewportStartOffset + 70 -> -18f
                dragY > info.viewportEndOffset - 70 -> 18f
                else -> 0f
            }
            if (step != 0f) {
                list.scrollBy(step)
                reorderAtPointer()
            }
            delay(32)
        }
    }
    Column(modifier) {
        Text(
            pluralStringResource(R.plurals.queue_count, queue.size, queue.size),
            Modifier.padding(vertical = 12.dp), color = SphereMuted,
        )
        LazyColumn(Modifier.weight(1f), state = list) {
            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                Row(
                    Modifier.fillMaxWidth().background(
                        if (dragging == song.id) SpherePurple.copy(alpha = 0.15f) else Color.Transparent,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        SongRow(song, song.id in state.favorites, song.id == state.current?.id,
                            { onPlay(index) }, { onFavorite(song) },
                            onMore = onSongActions?.let { action -> { action(song) } })
                    }
                    Icon(
                        Icons.Rounded.DragHandle,
                        stringResource(R.string.queue_drag, song.title),
                        Modifier.size(48.dp).semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(up) {
                                    queue.getOrNull(index - 1)?.let { move(song.id, it.id); true } ?: false
                                },
                                CustomAccessibilityAction(down) {
                                    queue.getOrNull(index + 1)?.let { move(song.id, it.id); true } ?: false
                                },
                            )
                        }.pointerInput(song.id) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = song.id
                                    lastTarget = null
                                    val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == song.id }
                                    dragY = item?.let { it.offset + it.size / 2f } ?: 0f
                                },
                                onDragEnd = { dragging = null },
                                onDragCancel = { dragging = null },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragY += amount.y
                                    reorderAtPointer()
                                },
                            )
                        }.padding(10.dp),
                        tint = SphereMuted,
                    )
                }
            }
        }
    }
}
