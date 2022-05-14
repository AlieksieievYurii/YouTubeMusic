package com.yurii.youtubemusic.utilities

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.services.mediaservice.MediaService
import com.yurii.youtubemusic.services.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.saved.SavedMusicViewModel
import com.yurii.youtubemusic.screens.youtube.YouTubeMusicViewModel
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

    fun provideSavedMusicViewModel(context: Context): SavedMusicViewModel.Factory {
        val applicationContext = context.applicationContext
        val musicServiceConnection = provideMusicServiceConnection(context)
        return SavedMusicViewModel.Factory(applicationContext, musicServiceConnection)
    }

    fun provideYouTubeMusicViewModel(context: Context, googleSignInAccount: GoogleSignInAccount): YouTubeMusicViewModel.Factory {
        val preferences = ServiceLocator.providePreferences(context)
        return YouTubeMusicViewModel.Factory(context, googleSignInAccount, preferences)
    }

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(context, ComponentName(context, MediaService::class.java))
    }
}