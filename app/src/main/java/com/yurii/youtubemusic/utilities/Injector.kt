package com.yurii.youtubemusic.utilities

import android.content.ComponentName
import android.content.Context
import com.yurii.youtubemusic.mediaservice.MediaService
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.viewmodels.savedmusic.SavedMusicViewModelFactory

object Injector {

    fun provideSavedMusicViewModel(context: Context): SavedMusicViewModelFactory {
        val applicationContext = context.applicationContext
        val musicServiceConnection = provideMusicServiceConnection(context)
        return SavedMusicViewModelFactory(applicationContext, musicServiceConnection)
    }

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(context, ComponentName(context, MediaService::class.java))
    }
}