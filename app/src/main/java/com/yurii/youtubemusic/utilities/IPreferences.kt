package com.yurii.youtubemusic.utilities

import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.models.AudioEffectsData
import com.yurii.youtubemusic.models.Category

interface IPreferences {
    fun getAudioEffectsData(): AudioEffectsData
    fun setAudioEffectsData(audioEffectsData: AudioEffectsData)
    fun getMusicCategories(): List<Category>
    fun setCategories(categories: List<Category>)
    fun setSelectedPlayList(playList: Playlist?)
    fun getSelectedPlayList(): Playlist?
}