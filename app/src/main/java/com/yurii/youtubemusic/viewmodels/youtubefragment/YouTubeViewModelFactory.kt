package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
class YouTubeViewModelFactory(private val application: Application, private val googleSignInAccount: GoogleSignInAccount) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeMusicViewModel::class.java))
            return YouTubeMusicViewModel(application, googleSignInAccount) as T
        throw IllegalStateException("Given the model class is not assignable from YouTuneViewModel class")
    }

}