package com.yurii.youtubemusic.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import java.lang.IllegalStateException

class PlayerBottomControllerViewModel(application: Application, musicServiceConnection: MusicServiceConnection) : AndroidViewModel(application)
{
    fun test() {
        Log.i("TEST", "OK")
    }
}

@Suppress("UNCHECKED_CAST")
class PlayerBottomControllerFactory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerBottomControllerViewModel::class.java))
            return PlayerBottomControllerViewModel(context as Application, musicServiceConnection) as T
        throw IllegalStateException("Given the model class is not assignable from PlayerBottomControllerViewModel class")
    }
}