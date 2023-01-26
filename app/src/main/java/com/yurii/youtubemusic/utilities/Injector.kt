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
import dagger.assisted.AssistedFactory

object Injector {
    fun provideMediaItemsViewModel(application: Application, category: Category): MediaItemsViewModel.Factory {
        return MediaItemsViewModel.Factory(
            category,
            MediaLibraryManager.getInstance(application),
            MediaServiceConnection.getInstance(application, QueueModesRepository.getInstance(application))
        )
    }
}