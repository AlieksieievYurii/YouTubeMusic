package com.yurii.youtubemusic.screens.youtube

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.utilities.GoogleAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

sealed  class Status {
    data class Download(val videoItemId: String) : Status()
    data class Downloaded(val videoItemId: String) : Status()
    data class Downloading(val videoItemId: String, val progress: Int) : Status()
}

class YouTubeMusicViewModel2(context: Context, googleSignInAccount: GoogleSignInAccount) : ViewModel() {
    private val credential = GoogleAccount.getGoogleAccountCredentialUsingOAuth2(googleSignInAccount, context)
    val s = YouTubeAPI(credential)

    val videoItems: MutableSharedFlow <PagingData<VideoItem>> = MutableSharedFlow ()
    private val _status = Channel<Status>()
    val status = _status.receiveAsFlow()

    private var searchJob: Job? = null

    fun setPlaylist(playlistId: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10), pagingSourceFactory = { VideosPagingSource(s, playlistId) }).flow.cachedIn(viewModelScope)
                .collectLatest {
                    videoItems.emit(it)
                }
        }
    }

    fun test() {
        viewModelScope.launch {
            _status.send(Status.Download("dsd"))
            delay(3000)
            _status.send(Status.Download("dsd2"))
            delay(3000)
            _status.send(Status.Download("dsd3"))
            delay(3000)
            _status.send(Status.Download("dsd4"))
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val context: Context, private val googleSignInAccount: GoogleSignInAccount) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(YouTubeMusicViewModel2::class.java))
                return YouTubeMusicViewModel2(context, googleSignInAccount) as T
            throw IllegalStateException("Given the model class is not assignable from YouTuneViewModel class")
        }

    }
}