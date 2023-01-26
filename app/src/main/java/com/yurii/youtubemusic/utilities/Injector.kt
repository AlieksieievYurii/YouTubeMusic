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
import com.yurii.youtubemusic.services.downloader.ServiceConnection
import com.yurii.youtubemusic.services.media.*

object Injector {

    fun provideEqualizerViewModel(application: Application): EqualizerViewModel.Factory {
        val audioEffectManager = AudioEffectManager.getInstance(Preferences.getInstance(application))
        return EqualizerViewModel.Factory(audioEffectManager)
    }

    fun provideMediaItemsViewModel(application: Application, category: Category): MediaItemsViewModel.Factory {
        return MediaItemsViewModel.Factory(
            category,
            MediaLibraryManager.getInstance(application),
            MediaServiceConnection.getInstance(application, QueueModesRepository.getInstance(application))
        )
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