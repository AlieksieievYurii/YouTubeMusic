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
import com.yurii.youtubemusic.screens.youtube.models.*
import com.yurii.youtubemusic.screens.youtube.service.ServiceConnection
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.GoogleAccount
import com.yurii.youtubemusic.utilities.Preferences2
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.IllegalStateException

abstract class VideoItemStatus(open val videoItem: Item) {
    class Download(override val videoItem: Item) : VideoItemStatus(videoItem)
    class Downloaded(override val videoItem: VideoItem, val size: Long) : VideoItemStatus(videoItem)
    class Downloading(override val videoItem: VideoItem, val currentSize: Long, val size: Long) : VideoItemStatus(videoItem)
    class Failed(override val videoItem: VideoItem, val error: Exception?) : VideoItemStatus(videoItem)
}

class YouTubeMusicViewModel(private val context: Context, googleSignInAccount: GoogleSignInAccount, private val preferences: Preferences2) :
    ViewModel() {
    sealed class Event {
        data class SelectCategories(val videoItem: VideoItem) : Event()
        data class DeleteItem(val videoItem: VideoItem) : Event()
        data class NotifyVideoItemHasBeenDeleted(val videoItem: VideoItem) : Event()
        data class NotifyVideoItemHasBeenDownloaded(val videoItem: VideoItem) : Event()
        data class ShowFailedVideoItem(val videoItem: VideoItem, val error: Exception?) : Event()
        object SignOut : Event()
    }

    private val credential = GoogleAccount.getGoogleAccountCredentialUsingOAuth2(googleSignInAccount, context)
    val youTubeAPI = YouTubeAPI(credential)

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems

    private val _currentPlaylistId: MutableStateFlow<Playlist?> = MutableStateFlow(preferences.getCurrentPlaylist())
    val currentPlaylistId: StateFlow<Playlist?> = _currentPlaylistId

    private val _videoItemStatus = MutableSharedFlow<VideoItemStatus>()
    val videoItemStatus: SharedFlow<VideoItemStatus> = _videoItemStatus

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private var searchJob: Job? = null

    private val downloaderServiceConnection = ServiceConnection(context)

    init {
        deleteUnFinishedJobs()
        _currentPlaylistId.value?.let { loadVideoItems(it) }
        downloaderServiceConnection.setCallbacks(object : ServiceConnection.CallBack {
            override fun onFinished(videoItem: VideoItem) {
                val musicFile = DataStorage.getMusic(context, videoItem.videoId)
                sendVideoItemStatus(VideoItemStatus.Downloaded(videoItem, musicFile.length()))
                sendEvent(Event.NotifyVideoItemHasBeenDownloaded(videoItem))
            }

            override fun onProgress(videoItem: VideoItem, progress: Progress) {
                sendVideoItemStatus(VideoItemStatus.Downloading(videoItem, progress.currentSize, progress.totalSize))
            }

            override fun onError(videoItem: VideoItem, error: Exception) {
                sendVideoItemStatus(VideoItemStatus.Failed(videoItem, error))
            }

        })
        downloaderServiceConnection.connect()
    }

    fun getAllCategories() = preferences.getMusicCategories()

    fun signOut() {
        GoogleAccount.signOut(context)
        preferences.setCurrentPlaylist(null)
        sendEvent(Event.SignOut)
    }

    fun setPlaylist(playlist: Playlist) {
        preferences.setCurrentPlaylist(playlist)
        _currentPlaylistId.value = playlist
        loadVideoItems(playlist)
    }

    fun download(item: VideoItem, categories: List<Category> = emptyList()) {
        downloaderServiceConnection.download(item, categories)
        sendVideoItemStatus(VideoItemStatus.Downloading(item, 0, 0))
    }

    fun tryToDownloadAgain(videoItem: VideoItem) {
        downloaderServiceConnection.retryToDownload(videoItem)
        sendVideoItemStatus(VideoItemStatus.Downloading(videoItem, 0, 0))
    }

    fun cancelDownloading(item: VideoItem) {
        downloaderServiceConnection.cancelDownloading(item)
        sendVideoItemStatus(VideoItemStatus.Download(item))
    }

    fun askToDelete(videoItem: VideoItem) = sendEvent(Event.DeleteItem(videoItem))

    fun delete(videoItem: VideoItem) {
        DataStorage.getMusic(context, videoItem.videoId).delete()
        DataStorage.getMetadata(context, videoItem.videoId).delete()
        DataStorage.getThumbnail(context, videoItem.videoId).delete()
        sendVideoItemStatus(VideoItemStatus.Download(videoItem))
        sendEvent(Event.NotifyVideoItemHasBeenDeleted(videoItem))
    }

    fun showFailedItemDetails(videoItem: VideoItem) = sendEvent(Event.ShowFailedVideoItem(videoItem, downloaderServiceConnection.getError(videoItem)))

    fun downloadAndAddToCategories(item: VideoItem) = sendEvent(Event.SelectCategories(item))

    fun getItemStatus(videoItem: VideoItem): VideoItemStatus {
        val musicFile = DataStorage.getMusic(context, videoItem.videoId)

        if (musicFile.exists())
            return VideoItemStatus.Downloaded(videoItem, musicFile.length())

        if (downloaderServiceConnection.isItemDownloading(videoItem)) {
            val progress = downloaderServiceConnection.getProgress(videoItem) ?: Progress.create()
            return VideoItemStatus.Downloading(videoItem, progress.currentSize, progress.totalSize)
        }

        if (downloaderServiceConnection.isDownloadingFailed(videoItem))
            return VideoItemStatus.Failed(videoItem, downloaderServiceConnection.getError(videoItem))

        return VideoItemStatus.Download(videoItem)
    }

    private fun deleteUnFinishedJobs() {
        DataStorage.getMusicStorage(context).walk().filter { it.extension == "downloading" }.forEach {
            it.delete()
        }
    }

    fun sendVideoItemStatus(videoItemStatus: VideoItemStatus) = viewModelScope.launch {
        _videoItemStatus.emit(videoItemStatus)
    }

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _event.emit(event)
    }

    private fun loadVideoItems(playlist: Playlist) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
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