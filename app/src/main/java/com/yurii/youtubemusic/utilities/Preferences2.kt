package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.screens.youtube.models.Playlist

class Preferences2(private val context: Context) {
    fun setCurrentPlaylist(playlist: Playlist) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE_2, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_CURRENT_PLAYLIST, Gson().toJson(playlist))
            apply()
        }
    }

    fun getCurrentPlaylist(): Playlist? {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE_2, Context.MODE_PRIVATE)
        val playlistJson: String = sharedPreferences.getString(KEY_CURRENT_PLAYLIST, null) ?: return null
        return Gson().fromJson(playlistJson, Playlist::class.java)
    }

    companion object {
        private const val DEFAULT_SHARED_PREFERENCES_FILE_2 = "com.yurii.youtubemusic.shared.preferences2"
        private const val KEY_CURRENT_PLAYLIST = "com.yurii.youtubemusic.shared.preferences.currentplaylist"
    }
}