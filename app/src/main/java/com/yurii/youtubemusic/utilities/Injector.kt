package com.yurii.youtubemusic.utilities

import android.content.ComponentName
import android.content.Context
import com.yurii.youtubemusic.mediaservice.MediaService
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.viewmodels.mediaitems.MediaItemsViewModel
import com.yurii.youtubemusic.viewmodels.savedmusic.SavedMusicViewModelFactory

object Injector {

    fun provideMediaItemsViewModel(context: Context, category: Category): MediaItemsViewModel {
        val musicServiceConnection = provideMusicServiceConnection(context)
        return MediaItemsViewModel(context, category, musicServiceConnection)
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