package com.yurii.youtubemusic.source

import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class YouTubePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    fun setCurrentYouTubePlaylist(playlist: Playlist?) {
        val sharedPreferences = context.getSharedPreferences(YOUTUBE_PREFERENCES, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_CURRENT_PLAYLIST, Gson().toJson(playlist))
            apply()
        }
    }

    fun getCurrentYouTubePlaylist(): Playlist? {
        val sharedPreferences = context.getSharedPreferences(YOUTUBE_PREFERENCES, Context.MODE_PRIVATE)
        val playlistJson: String = sharedPreferences.getString(KEY_CURRENT_PLAYLIST, null) ?: return null
        return Gson().fromJson(playlistJson, Playlist::class.java)
    }

    companion object {
        private const val YOUTUBE_PREFERENCES = "com.yurii.youtubemusic.shared.preferences.youtube"
        private const val KEY_CURRENT_PLAYLIST = "com.yurii.youtubemusic.shared.preferences.currentplaylist"
    }
}