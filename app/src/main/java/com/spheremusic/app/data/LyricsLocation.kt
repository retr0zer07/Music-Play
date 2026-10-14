package com.spheremusic.app.data

object LyricsLocation {
    fun siblingDirectory(authority: String, treeId: String, volume: String, relativePath: String): String? {
        if (authority != "com.android.externalstorage.documents" || relativePath.isBlank()) return null
        val storage = when (volume) {
            "external_primary" -> "primary"
            "", "external", "internal" -> return null
            else -> volume
        }
        val treeStorage = treeId.substringBefore(':')
        if (!storage.equals(treeStorage, ignoreCase = true)) return null
        val directory = relativePath.trim('/')
        if (directory.split('/').any { it == ".." || it == "." }) return null
        val allowed = treeId.substringAfter(':', "").trim('/')
        if (allowed.isNotEmpty() && directory != allowed && !directory.startsWith("$allowed/")) return null
        return "$treeStorage:$directory"
    }
}
