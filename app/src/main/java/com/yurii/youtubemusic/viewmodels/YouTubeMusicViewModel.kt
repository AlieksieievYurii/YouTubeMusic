package com.yurii.youtubemusic.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.GoogleAccount
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.services.youtube.YouTubeService
import com.yurii.youtubemusic.utilities.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.ArrayList

interface VideosLoader {
    fun onResult(newVideos: List<VideoItem>)
    fun onError(error: Exception)
}

interface VideoItemChange {
    fun onChangeProgress(videoItem: VideoItem, progress: Progress)
    fun onDownloadingFinished(videoItem: VideoItem)
    fun onDownloadingFailed(videoItem: VideoItem, error: Exception)
}

class YouTubeMusicViewModel(application: Application, googleSignInAccount: GoogleSignInAccount) : AndroidViewModel(application) {
    private val youTubeService: IYouTubeService
    private val context: Context = getApplication<Application>().baseContext
    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    val selectedPlaylist: LiveData<Playlist?> get() = _selectedPlayList
    private var videoLoadingCanceler: ICanceler? = null
    private var nextPageToken: String? = null

    var videosLoader: VideosLoader? = null
    var videoItemChange: VideoItemChange? = null

    init {
        val credential = GoogleAccount.getGoogleAccountCredentialUsingOAuth2(googleSignInAccount, context)
        youTubeService = YouTubeService(credential)

        loadVideosIfAlreadySelectedPlaylist()
    }

    private fun loadVideosIfAlreadySelectedPlaylist() {
        val currentSelectedPlaylist = Preferences.getSelectedPlayList(context)
        _selectedPlayList.value = currentSelectedPlaylist

        if (currentSelectedPlaylist != null)
            loadVideos(null)
    }

    fun onReceive(intent: Intent) {
        when (intent.action) {
            MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION -> {
                val videoItem: VideoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as VideoItem
                val progress: Progress = intent.getSerializableExtra(MusicDownloaderService.EXTRA_PROGRESS) as Progress
                videoItemChange?.onChangeProgress(videoItem, progress)
            }
            MusicDownloaderService.DOWNLOADING_FINISHED_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as VideoItem
                videoItemChange?.onDownloadingFinished(videoItem)
            }
            MusicDownloaderService.DOWNLOADING_FAILED_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as VideoItem
                val error = intent.getSerializableExtra(MusicDownloaderService.EXTRA_ERROR) as Exception
                videoItemChange?.onDownloadingFailed(videoItem, error)
            }
        }
    }

    fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String? = null): ICanceler {
        Log.i(LOG_TAG, "Start loading playLists with next page token $nextPageToken")
        return youTubeService.loadPlayLists(observer, nextPageToken)
    }

    fun signOut() {
        GoogleAccount.signOut(context)
        cleanData()
    }

    private fun cleanData() {
        Preferences.setSelectedPlayList(context, null)
        videoLoadingCanceler?.cancel()
    }

    fun setNewPlayList(playlist: Playlist) {
        if (playlist != _selectedPlayList.value) {
            Preferences.setSelectedPlayList(context, playlist)
            _selectedPlayList.value = playlist
            nextPageToken = null
            loadVideos(nextPageToken)
        }
    }

    fun loadMoreVideos() {
        loadVideos(nextPageToken)
    }

    private fun loadVideos(nextPageToken: String?) {
        videoLoadingCanceler?.cancel()

        videoLoadingCanceler = youTubeService.loadPlayListItems(_selectedPlayList.value!!.id, object : YouTubeObserver<PlaylistItemListResponse> {
            override fun onResult(result: PlaylistItemListResponse) {
                this@YouTubeMusicViewModel.nextPageToken = result.nextPageToken
                retrieveVideos(result)
            }

            override fun onError(error: Exception) {
                videosLoader?.onError(error)
            }
        }, nextPageToken)
    }

    fun exists(videoItem: VideoItem): Boolean = DataStorage.getMusic(context, videoItem.videoId).exists()

    fun isVideoItemLoading(videoItem: VideoItem): Boolean = MusicDownloaderService.Instance.serviceInterface?.isLoading(videoItem) ?: false

    fun getCurrentProgress(videoItem: VideoItem) = MusicDownloaderService.Instance.serviceInterface?.getProgress(videoItem)

    fun startDownloadMusic(videoItem: VideoItem, categories: ArrayList<Category> = ArrayList()) {
        context.startService(Intent(context, MusicDownloaderService::class.java).also {
            it.putExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM, videoItem)
            it.putParcelableArrayListExtra(MusicDownloaderService.EXTRA_CATEGORIES, categories)
        })
    }

    fun stopDownloading(videoItem: VideoItem) {
        MusicDownloaderService.Instance.serviceInterface?.cancel(videoItem)
    }

    fun removeVideoItem(videoItem: VideoItem) {
        DataStorage.getMusic(context, videoItem.videoId).delete()
        DataStorage.getMetadata(context, videoItem.videoId).delete()
        DataStorage.getThumbnail(context, videoItem.videoId).delete()
    }

    fun isVideoPageLast() = nextPageToken.isNullOrEmpty()

    private fun retrieveVideos(playlistItemListResponse: PlaylistItemListResponse) {
        youTubeService.loadVideosDetails(
            playlistItemListResponse.items.map { it.snippet.resourceId.videoId },
            object : YouTubeObserver<VideoListResponse> {
                override fun onResult(result: VideoListResponse) {
                    val videoItems = result.items.map { VideoItem.createFrom(it) }
                    videosLoader?.onResult(videoItems)
                    videoLoadingCanceler = null
                }

                override fun onError(error: Exception) {
                    videosLoader?.onError(error)
                }
            })
    }

    companion object {
        private const val LOG_TAG: String = "YouTubeViewModel"
    }
}

@Suppress("UNCHECKED_CAST")
class YouTubeViewModelFactory(private val application: Application, private val googleSignInAccount: GoogleSignInAccount) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeMusicViewModel::class.java))
            return YouTubeMusicViewModel(application, googleSignInAccount) as T
        throw IllegalStateException("Given the model class is not assignable from YouTuneViewModel class")
    }

}