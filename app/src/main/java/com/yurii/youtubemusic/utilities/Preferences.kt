package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.models.EqualizerData
import com.yurii.youtubemusic.models.TwisterData
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Preferences @Inject constructor(@ApplicationContext private val context: Context) {

    fun getBassBoostData(): TwisterData {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_BASS_BOOST_DATA, null) ?: return TwisterData.create()
        return Gson().fromJson(audioEffectsDataJson, TwisterData::class.java)
    }

    fun setBassBoostData(bassBoostData: TwisterData) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_BASS_BOOST_DATA, Gson().toJson(bassBoostData))
            apply()
        }
    }

    fun getVirtualizerData(): TwisterData {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_VIRTUALIZER_DATA, null) ?: return TwisterData.create()
        return Gson().fromJson(audioEffectsDataJson, TwisterData::class.java)
    }

    fun setVirtualizerData(virtualizerData: TwisterData) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_VIRTUALIZER_DATA, Gson().toJson(virtualizerData))
            apply()
        }
    }

    fun getEqualizerData(): EqualizerData {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_EQUALIZER_DATA, null) ?: return EqualizerData.create()
        return Gson().fromJson(audioEffectsDataJson, EqualizerData::class.java)
    }

    fun setEqualizerData(equalizerData: EqualizerData) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_EQUALIZER_DATA, Gson().toJson(equalizerData))
            apply()
        }
    }

    fun setCurrentYouTubePlaylist(playlist: Playlist?) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_CURRENT_PLAYLIST, Gson().toJson(playlist))
            apply()
        }
    }

    fun getCurrentYouTubePlaylist(): Playlist? {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val playlistJson: String = sharedPreferences.getString(KEY_CURRENT_PLAYLIST, null) ?: return null
        return Gson().fromJson(playlistJson, Playlist::class.java)
    }

    companion object {
        private const val DEFAULT_SHARED_PREFERENCES_FILE = "com.yurii.youtubemusic.shared.preferences"
        private const val KEY_CURRENT_PLAYLIST = "com.yurii.youtubemusic.shared.preferences.currentplaylist"
        private const val SH_KEY_BASS_BOOST_DATA: String = "com.yurii.youtubemusic.bass.boost"
        private const val SH_KEY_VIRTUALIZER_DATA: String = "com.yurii.youtubemusic.virtualizer"
        private const val SH_KEY_EQUALIZER_DATA: String = "com.yurii.youtubemusic.bass.equalizer"
    }
}