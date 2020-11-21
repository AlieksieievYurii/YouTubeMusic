package com.yurii.youtubemusic.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.yurii.youtubemusic.models.AudioEffectsData
import com.yurii.youtubemusic.services.mediaservice.MusicServiceConnection
import java.lang.IllegalStateException

class EqualizerViewModel(application: Application, musicServiceConnection: MusicServiceConnection) : AndroidViewModel(application) {
    val audioEffectManager = musicServiceConnection.audioEffectManager
}

@Suppress("UNCHECKED_CAST")
class EqualizerViewModelFactory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EqualizerViewModel::class.java))
            return EqualizerViewModel(context as Application, musicServiceConnection) as T
        throw IllegalStateException("Given the model class is not assignable from EqualizerViewModel class")
    }
}