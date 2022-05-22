package com.yurii.youtubemusic.screens.equalizer

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.yurii.youtubemusic.screens.saved.service.MusicServiceConnection
import java.lang.IllegalStateException

class EqualizerViewModel(application: Application, musicServiceConnection: MusicServiceConnection) : AndroidViewModel(application) {
    val audioEffectManager = musicServiceConnection.audioEffectManager

    @Suppress("UNCHECKED_CAST")
    class Factory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EqualizerViewModel::class.java))
                return EqualizerViewModel(context as Application, musicServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from EqualizerViewModel class")
        }
    }
}

