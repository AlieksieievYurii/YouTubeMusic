package com.yurii.youtubemusic.viewmodels.savedmusic

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
class SavedMusicViewModelFactory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedMusicViewModel::class.java))
            return SavedMusicViewModel(context as Application, musicServiceConnection) as T
        throw IllegalStateException("Given the model class is not assignable from SavedMusicViewModel class")
    }
}