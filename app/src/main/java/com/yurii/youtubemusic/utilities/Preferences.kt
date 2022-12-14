package com.yurii.youtubemusic.utilities

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.models.AudioEffectsData
import com.yurii.youtubemusic.screens.youtube.models.Playlist
import com.yurii.youtubemusic.services.media.MediaServiceConnection

class Preferences private constructor(private val application: Application) {

    fun getAudioEffectsData(): AudioEffectsData {
        val sharedPreferences = application.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_AUDIO_EFFECTS_DATA, null) ?: return AudioEffectsData.create()
        return Gson().fromJson(audioEffectsDataJson, AudioEffectsData::class.java)
    }

    fun setAudioEffectsData(audioEffectsData: AudioEffectsData) {
        val sharedPreferences = application.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_AUDIO_EFFECTS_DATA, Gson().toJson(audioEffectsData))
            apply()
        }
    }

    fun setCurrentYouTubePlaylist(playlist: Playlist?) {
        val sharedPreferences = application.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_CURRENT_PLAYLIST, Gson().toJson(playlist))
            apply()
        }
    }

    fun getCurrentYouTubePlaylist(): Playlist? {
        val sharedPreferences = application.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val playlistJson: String = sharedPreferences.getString(KEY_CURRENT_PLAYLIST, null) ?: return null
        return Gson().fromJson(playlistJson, Playlist::class.java)
    }

    companion object {
        private const val DEFAULT_SHARED_PREFERENCES_FILE = "com.yurii.youtubemusic.shared.preferences2"
        private const val KEY_CURRENT_PLAYLIST = "com.yurii.youtubemusic.shared.preferences.currentplaylist"
        private const val SH_KEY_AUDIO_EFFECTS_DATA: String = "com.yurii.youtubemusic.shared.preferences.audio.effects.data"


        @Volatile
        private var instance: Preferences? = null

        fun getInstance(application: Application): Preferences {
            if (instance == null)
                synchronized(this) {
                    instance = Preferences(application)
                }
            return instance!!
        }
    }
}