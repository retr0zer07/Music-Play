package com.spheremusic.app.data

import android.content.SharedPreferences
import java.io.IOException
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: String, val name: String, val songIds: List<Long> = emptyList())

class PlaylistStore(private val preferences: SharedPreferences) {
    @Synchronized fun load(): List<Playlist> {
        val array = JSONArray(preferences.getString("playlists", "[]"))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val songs = item.getJSONArray("songs")
            Playlist(item.getString("id"), item.getString("name"), List(songs.length()) { songs.getLong(it) })
        }
    }

    @Synchronized fun create(name: String, songId: Long? = null): List<Playlist> {
        val playlists = load()
        val title = name.trim()
        require(title.isNotEmpty() && playlists.none { it.name.equals(title, ignoreCase = true) })
        return save(playlists + Playlist(UUID.randomUUID().toString(), title, listOfNotNull(songId)))
    }

    @Synchronized fun rename(id: String, name: String): List<Playlist> {
        val playlists = load()
        val title = name.trim()
        require(title.isNotEmpty() && playlists.none { it.id != id && it.name.equals(title, ignoreCase = true) })
        require(playlists.any { it.id == id })
        return save(playlists.map { if (it.id == id) it.copy(name = title) else it })
    }

    @Synchronized fun addSong(id: String, songId: Long): List<Playlist> {
        return addSongs(id, listOf(songId))
    }

    @Synchronized fun addSongs(id: String, songIds: List<Long>): List<Playlist> {
        val playlists = load()
        require(playlists.any { it.id == id })
        return save(playlists.map {
            if (it.id == id) it.copy(songIds = (it.songIds + songIds).distinct()) else it
        })
    }

    @Synchronized fun removeSong(id: String, songId: Long): List<Playlist> =
        save(load().map { if (it.id == id) it.copy(songIds = it.songIds - songId) else it })

    @Synchronized fun removeSongEverywhere(songId: Long): List<Playlist> =
        save(load().map { it.copy(songIds = it.songIds - songId) })

    @Synchronized fun delete(id: String): List<Playlist> = save(load().filter { it.id != id })

    private fun save(playlists: List<Playlist>): List<Playlist> {
        val array = JSONArray()
        playlists.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("songs", JSONArray(it.songIds)))
        }
        if (!preferences.edit().putString("playlists", array.toString()).commit()) {
            throw IOException("Cannot save playlists")
        }
        return playlists
    }
}
