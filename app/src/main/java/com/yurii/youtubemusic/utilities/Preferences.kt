package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.model.Playlist
import com.google.gson.Gson
import com.yurii.youtubemusic.models.AudioEffectsData
import com.yurii.youtubemusic.models.Category

const val DEFAULT_SHARED_PREFERENCES_FILE: String = "com.yurii.youtubemusic.shared.preferences"
const val SHARED_PREFERENCES_SELECTED_PLAY_LIST: String = "com.yurii.youtubemusic.shared.preferences.selected.play.list"
const val SH_KEY_MUSICS_CATEGORIES: String = "com.yurii.youtubemusic.shared.preferences.music.categories"
const val SH_KEY_AUDIO_EFFECTS_DATA: String = "com.yurii.youtubemusic.shared.preferences.audio.effects.data"

class Preferences(private val context: Context) : IPreferences {
    override fun getAudioEffectsData(): AudioEffectsData {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_AUDIO_EFFECTS_DATA, null) ?: return AudioEffectsData.create()
        return Gson().fromJson(audioEffectsDataJson, AudioEffectsData::class.java)
    }

    override fun setAudioEffectsData(audioEffectsData: AudioEffectsData) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_AUDIO_EFFECTS_DATA, Gson().toJson(audioEffectsData))
            apply()
        }
    }

    override fun getMusicCategories(): List<Category> {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val categoriesJson: String = sharedPreferences.getString(SH_KEY_MUSICS_CATEGORIES, null) ?: return emptyList()
        return Gson().fromJson(categoriesJson, Array<Category>::class.java).toList()
    }

    override fun setCategories(categories: List<Category>) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_MUSICS_CATEGORIES, Gson().toJson(categories))
            apply()
        }
    }

    override fun setSelectedPlayList(playList: Playlist?) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, playList?.toString())
            apply()
        }
    }

    override fun getSelectedPlayList(): Playlist? {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val jsonRepresentation: String? = sharedPreferences.getString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, null)
        jsonRepresentation?.let {
            val jsonFactory: com.google.api.client.json.JsonFactory = JacksonFactory.getDefaultInstance()
            return jsonFactory.fromString(it, Playlist::class.java)
        } ?: return null
    }
}