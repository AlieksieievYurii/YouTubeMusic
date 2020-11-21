package com.yurii.youtubemusic.utilities

import android.content.ComponentName
import android.content.Context
import com.yurii.youtubemusic.services.mediaservice.MediaService
import com.yurii.youtubemusic.services.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.viewmodels.*

object Injector {

    fun provideEqualizerViewModel(context: Context): EqualizerViewModelFactory {
        val musicServiceConnection = provideMusicServiceConnection(context)
        return EqualizerViewModelFactory(context.applicationContext, musicServiceConnection)
    }

    fun provideMediaItemsViewModel(context: Context, category: Category): MediaItemsViewModel {
        val musicServiceConnection = provideMusicServiceConnection(context)
        return MediaItemsViewModel(context, category, musicServiceConnection)
    }

    fun providePlayerControllerViewModel(context: Context): PlayerBottomControllerFactory {
        val applicationContext = context.applicationContext
        val musicServiceConnection = provideMusicServiceConnection(context)
        return PlayerBottomControllerFactory(applicationContext, musicServiceConnection)
    }

    fun provideSavedMusicViewModel(context: Context): SavedMusicViewModelFactory {
        val applicationContext = context.applicationContext
        val musicServiceConnection = provideMusicServiceConnection(context)
        return SavedMusicViewModelFactory(applicationContext, musicServiceConnection)
    }

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(context, ComponentName(context, MediaService::class.java))
    }
}