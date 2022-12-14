package com.yurii.youtubemusic.utilities

import android.app.Application
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
        return EqualizerViewModel.Factory()
    }

    fun providePlayerControllerViewModel(context: Context): PlayerControllerViewModel.Factory {
        return PlayerControllerViewModel.Factory(MediaServiceConnection.getInstance(context))
    }

    fun provideSavedMusicViewModel(context: Context): SavedMusicViewModel.Factory {
        return SavedMusicViewModel.Factory(MediaServiceConnection.getInstance(context), MediaLibraryManager.getInstance(context))
    }

    fun provideMediaItemsViewModel(context: Context, category: Category): MediaItemsViewModel.Factory {
        val mediaPlayer = MediaPlayer(category, MediaServiceConnection.getInstance(context), MediaLibraryManager.getInstance(context))
        return MediaItemsViewModel.Factory(MediaLibraryManager.getInstance(context), mediaPlayer)
    }

    fun provideYouTubeMusicViewModel(application: Application, googleSignInAccount: GoogleSignInAccount): YouTubeMusicViewModel.Factory {
        val googleAccount = GoogleAccount(application)
        val downloaderServiceConnection = ServiceConnection(application)
        return YouTubeMusicViewModel.Factory(
            MediaLibraryManager.getInstance(application),
            googleAccount,
            downloaderServiceConnection,
            googleSignInAccount,
            Preferences.getInstance(application)
        )
    }

    fun provideCategoriesEditorViewMode(context: Context): CategoriesEditorViewModel.Factory {
        return CategoriesEditorViewModel.Factory(MediaLibraryManager.getInstance(context))
    }
}