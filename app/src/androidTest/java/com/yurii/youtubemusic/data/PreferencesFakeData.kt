package com.yurii.youtubemusic.data

import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.models.AudioEffectsData
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.IPreferences

class PreferencesFakeData : IPreferences {

    private var audioEffectsData: AudioEffectsData? = null
    private var categories: List<Category> = emptyList()
    private var selectedPlaylist: Playlist? = null

    override fun getAudioEffectsData(): AudioEffectsData = audioEffectsData ?: AudioEffectsData.create()

    override fun setAudioEffectsData(audioEffectsData: AudioEffectsData) {
        this.audioEffectsData = audioEffectsData
    }

    override fun getMusicCategories(): List<Category> = categories

    override fun setCategories(categories: List<Category>) {
        this.categories = categories
    }

    override fun setSelectedPlayList(playList: Playlist?) {
        this.selectedPlaylist = playList
    }

    override fun getSelectedPlayList(): Playlist? = selectedPlaylist
}