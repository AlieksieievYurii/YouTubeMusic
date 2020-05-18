package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
class YouTubeViewModelFactory(private val application: Application, private val youTubeService: IYouTubeService) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeMusicViewModel::class.java))
            return YouTubeMusicViewModel(application, youTubeService) as T
        throw IllegalStateException("Given the model class is not assignable from YouTuneViewModel class")
    }

}