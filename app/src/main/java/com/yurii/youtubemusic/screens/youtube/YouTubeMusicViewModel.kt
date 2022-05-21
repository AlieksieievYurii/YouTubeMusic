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
import com.yurii.youtubemusic.screens.youtube.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Playlist
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.screens.youtube.service.ServiceConnection
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.GoogleAccount
import com.yurii.youtubemusic.utilities.Preferences2
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.IllegalStateException

abstract class VideoItemStatus(open val videoItemId: String) {
    class Download(override val videoItemId: String) : VideoItemStatus(videoItemId)
    class Downloaded(override val videoItemId: String, val size: Float) : VideoItemStatus(videoItemId)
    class Downloading(override val videoItemId: String, val progress: Int, val size: Float) : VideoItemStatus(videoItemId)
    class Failed(override val videoItemId: String) : VideoItemStatus(videoItemId)
}

class YouTubeMusicViewModel(private val context: Context, googleSignInAccount: GoogleSignInAccount, private val preferences: Preferences2) :
    ViewModel() {
    sealed class Event {
        data class SelectCategories(val videoItem: VideoItem) : Event()
    }

    private val credential = GoogleAccount.getGoogleAccountCredentialUsingOAuth2(googleSignInAccount, context)
    val youTubeAPI = YouTubeAPI(credential)

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems

    private val _currentPlaylistId: MutableStateFlow<Playlist?> = MutableStateFlow(preferences.getCurrentPlaylist())
    val currentPlaylistId: StateFlow<Playlist?> = _currentPlaylistId

    private val _videoItemStatus = Channel<VideoItemStatus>()
    val videoItemStatus = _videoItemStatus.receiveAsFlow()

    private val _event = Channel<Event>()
    val event = _event.receiveAsFlow()

    private var searchJob: Job? = null

    private val downloaderServiceConnection = ServiceConnection(context)

    init {
        _currentPlaylistId.value?.let { loadVideoItems(it) }
        downloaderServiceConnection.setCallbacks(object : ServiceConnection.CallBack {
            override fun onFinished(videoItem: VideoItem) {
                viewModelScope.launch {
                    _videoItemStatus.send(VideoItemStatus.Downloaded(videoItem.videoId, 100f))
                }
            }

            override fun onProgress(videoItem: VideoItem, progress: Progress) {
                viewModelScope.launch {
                    _videoItemStatus.send(VideoItemStatus.Downloading(videoItem.videoId, progress.progress, progress.totalSizeInMb.toFloat()))
                }
            }

            override fun onError(videoItem: VideoItem, error: Exception) {
                throw error
            }

        })
        downloaderServiceConnection.connect()
    }

    fun setPlaylist(playlist: Playlist) {
        preferences.setCurrentPlaylist(playlist)
        _currentPlaylistId.value = playlist
        loadVideoItems(playlist)
    }

    fun download(item: VideoItem, categories: List<Category> = emptyList()) {
        downloaderServiceConnection.download(item, categories)
        sendVideoItemStatus(VideoItemStatus.Downloading(item.videoId, 0, 0f))
    }

    fun cancelDownloading(item: VideoItem) {
        downloaderServiceConnection.cancelDownloading(item)
        sendVideoItemStatus(VideoItemStatus.Download(item.videoId))
    }

    fun delete(item: VideoItem) {
        DataStorage.getMusic(context, item.videoId).delete()
        DataStorage.getMetadata(context, item.videoId).delete()
        DataStorage.getThumbnail(context, item.videoId).delete()
        sendVideoItemStatus(VideoItemStatus.Download(item.videoId))
    }

    fun openIssue(item: VideoItem) {

    }

    fun downloadAndAddToCategories(item: VideoItem) = viewModelScope.launch {
        _event.send(Event.SelectCategories(item))
    }

    private fun exists(videoItem: VideoItem): Boolean = DataStorage.getMusic(context, videoItem.videoId).exists()

    fun getItemStatus(item: VideoItem): VideoItemStatus {
        if (exists(item))
            return VideoItemStatus.Downloaded(item.videoId, 300f)

        if (downloaderServiceConnection.isDownloadingFailed(item)) {
            val p = downloaderServiceConnection.getProgress(item) ?: Progress.create()
            return VideoItemStatus.Downloading(item.videoId, p.progress, p.totalSizeInMb.toFloat())
        }

        if (downloaderServiceConnection.isDownloadingFailed(item))
            return VideoItemStatus.Failed(item.videoId)

        return VideoItemStatus.Download(item.videoId)
    }

    private fun sendVideoItemStatus(videoItemStatus: VideoItemStatus) = viewModelScope.launch {
        _videoItemStatus.send(videoItemStatus)
    }

    private fun loadVideoItems(playlist: Playlist) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10),
                pagingSourceFactory = { VideosPagingSource(youTubeAPI, playlist.id) }).flow.cachedIn(viewModelScope)
                .collectLatest {
                    _videoItems.emit(it)
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val context: Context, private val googleSignInAccount: GoogleSignInAccount, private val preferences: Preferences2) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(YouTubeMusicViewModel::class.java))
                return YouTubeMusicViewModel(context, googleSignInAccount, preferences) as T
            throw IllegalStateException("Given the model class is not assignable from YouTuneViewModel class")
        }

    }
}