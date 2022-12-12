package com.yurii.youtubemusic.utilities

import android.content.ComponentName
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.screens.saved.service.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.categories.CategoriesEditorViewModel
import com.yurii.youtubemusic.screens.equalizer.EqualizerViewModel
import com.yurii.youtubemusic.screens.player.PlayerControllerViewModel
import com.yurii.youtubemusic.screens.saved.SavedMusicViewModel
import com.yurii.youtubemusic.screens.saved.mediaitems.MediaItemsViewModel
import com.yurii.youtubemusic.screens.youtube.YouTubeMusicViewModel
import com.yurii.youtubemusic.screens.youtube.service.ServiceConnection
import com.yurii.youtubemusic.services.media.*

object Injector {

    fun provideEqualizerViewModel(context: Context): EqualizerViewModel.Factory {
        val musicServiceConnection = provideMusicServiceConnection(context)
        return EqualizerViewModel.Factory(context.applicationContext, musicServiceConnection)
    }

    fun providePlayerControllerViewModel(context: Context): PlayerControllerViewModel.Factory {
        return PlayerControllerViewModel.Factory(MediaServiceConnection.getInstance(context))
    }

    fun provideSavedMusicViewModel(context: Context): SavedMusicViewModel.Factory {
        return SavedMusicViewModel.Factory(MediaServiceConnection.getInstance(context))
    }

    fun provideMediaItemsViewModel(context: Context, category: Category): MediaItemsViewModel.Factory {
        val mediaPlayer = MediaPlayer(category, MediaServiceConnection.getInstance(context), MediaLibraryManager.getInstance(context))
        return MediaItemsViewModel.Factory(mediaPlayer)
    }

    fun provideYouTubeMusicViewModel(context: Context, googleSignInAccount: GoogleSignInAccount): YouTubeMusicViewModel.Factory {
        val mediaStorage = MediaStorage(context)
        val googleAccount = GoogleAccount(context)
        val downloaderServiceConnection = ServiceConnection(context)
        return YouTubeMusicViewModel.Factory(mediaStorage, googleAccount, downloaderServiceConnection, googleSignInAccount, Preferences(context))
    }

    fun provideCategoriesEditorViewMode(context: Context): CategoriesEditorViewModel.Factory {
        val preferences = ServiceLocator.providePreferences(context)
        return CategoriesEditorViewModel.Factory(preferences)
    }

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(context, ComponentName(context, MediaService::class.java))
    }
}