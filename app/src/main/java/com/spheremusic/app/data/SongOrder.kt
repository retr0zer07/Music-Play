package com.spheremusic.app.data

import java.util.Locale

enum class SongOrder {
    RECENT, TITLE;

    fun sorted(songs: List<Song>): List<Song> = when (this) {
        RECENT -> songs.sortedWith(compareByDescending<Song> { it.dateAddedSeconds }.thenByDescending { it.id })
        TITLE -> songs.sortedWith(compareBy<Song> { it.title.lowercase(Locale.ROOT) }.thenBy { it.id })
    }
}
