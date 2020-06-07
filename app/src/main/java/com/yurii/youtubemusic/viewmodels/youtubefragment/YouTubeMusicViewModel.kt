package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.youtube.YouTubeScopes
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.services.youtube.YouTubeService
import com.yurii.youtubemusic.utilities.*
import java.lang.Exception
import java.lang.RuntimeException

interface VideosLoader {
    fun onResult(newVideos: List<VideoItem>)
    fun onError(error: Exception)
}

interface VideoItemChange {
    fun onChangeProgress(videoItem: VideoItem, progress: Progress)
    fun onDownloadingFinished(videoItem: VideoItem)
}

class YouTubeMusicViewModel(application: Application, private val googleSignInAccount: GoogleSignInAccount) : AndroidViewModel(application) {
    private val mYouTubeService: IYouTubeService = YouTubeService()
    private val mContext: Context = getApplication<Application>().baseContext
    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    val selectedPlaylist: LiveData<Playlist?> get() = _selectedPlayList
    private var mVideoLoadingCanceler: ICanceler? = null
    private val mVideos: MutableList<VideoItem> = mutableListOf()
    private var mNextPageToken: String? = null
    private val allDownloadedMusics: MutableList<String> = mutableListOf()

    var mVideosLoader: VideosLoader? = null
    var mVideoItemChange: VideoItemChange? = null

    init {
        val credential = GoogleAccountCredential.usingOAuth2(application.baseContext, listOf(YouTubeScopes.YOUTUBE)).apply {
            selectedAccount = googleSignInAccount.account
        }

        mYouTubeService.setCredentials(credential)

        DataStorage.getMusicStorage(mContext).walk().forEach {
            Regex("(.+?)(\\.mp3\$)").find(it.name)?.groups?.get(1)?.value?.let { value ->
                allDownloadedMusics.add(value)
            }
        }

        _selectedPlayList.value = Preferences.getSelectedPlayList(mContext)
        if (_selectedPlayList.value != null)
            loadVideos(null)
    }

    fun onReceive(intent: Intent) {
        when (intent.action) {
            MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION -> {
                val videoItem: VideoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as VideoItem
                val progress: Progress = intent.getSerializableExtra(MusicDownloaderService.EXTRA_PROGRESS) as Progress
                mVideoItemChange?.onChangeProgress(videoItem, progress)
            }
            MusicDownloaderService.DOWNLOADING_FINISHED_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as VideoItem
                allDownloadedMusics.add(videoItem.also {
                    addTag(videoItem)
                    mVideoItemChange?.onDownloadingFinished(videoItem)
                }.videoId!!)
            }
        }
    }

    private fun addTag(videoItem: VideoItem) {
        val file = DataStorage.getMusic(mContext, videoItem)
        val tag = Tag(title = videoItem.title, authorChannel = videoItem.authorChannelTitle)
        TaggerV1(file).writeTag(tag)
    }

    fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String? = null) {
        Log.i(LOG_TAG, "Start loading playLists with next page token $nextPageToken")
        mYouTubeService.loadPlayLists(observer, nextPageToken)
    }

    fun setNewPlayList(playlist: Playlist) {
        if (playlist != _selectedPlayList.value) {
            Preferences.setSelectedPlayList(mContext, playlist)
            _selectedPlayList.value = playlist
            mVideos.clear()
            mNextPageToken = null
            loadVideos(mNextPageToken)
        }
    }

    fun loadMoreVideos() {
        loadVideos(mNextPageToken)
    }

    private fun loadVideos(nextPageToken: String?) {
        mVideoLoadingCanceler?.cancel()

        mVideoLoadingCanceler = mYouTubeService.loadPlayListItems(_selectedPlayList.value!!.id, object : YouTubeObserver<PlaylistItemListResponse> {
            override fun onResult(result: PlaylistItemListResponse) {
                this@YouTubeMusicViewModel.mNextPageToken = result.nextPageToken
                retrieveVideos(result)
            }

            override fun onError(error: Exception) {
                mVideosLoader?.onError(error)
            }
        }, nextPageToken)
    }

    fun isExist(videoItem: VideoItem) = allDownloadedMusics.contains(videoItem.videoId)

    fun isVideoItemLoading(videoItem: VideoItem): Boolean = MusicDownloaderService.Instance.serviceInterface?.isLoading(videoItem) ?: false

    fun getCurrentProgress(videoItem: VideoItem) = MusicDownloaderService.Instance.serviceInterface?.getProgress(videoItem)

    fun startDownloadingMusic(videoItem: VideoItem) {
        mContext.startService(Intent(mContext, MusicDownloaderService::class.java).also {
            it.putExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM, videoItem)
        })
    }

    fun stopDownloading(videoItem: VideoItem) = MusicDownloaderService.Instance.serviceInterface?.cancel(videoItem)

    fun removeVideoItem(videoItem: VideoItem) {
        val file = DataStorage.getMusic(mContext, videoItem)
        allDownloadedMusics.remove(videoItem.videoId)
        if (!file.delete())
            throw RuntimeException("Cannot remove the music file $file")
    }

    fun getCurrentVideos() = mVideos
    fun isVideoPageLast() = mNextPageToken.isNullOrEmpty()

    private fun retrieveVideos(playlistItemListResponse: PlaylistItemListResponse) {
        mYouTubeService.loadVideosDetails(
            playlistItemListResponse.items.map { it.snippet.resourceId.videoId },
            object : YouTubeObserver<VideoListResponse> {
                override fun onResult(result: VideoListResponse) {
                    val videoItems = result.items.map {
                        VideoItem(
                            videoId = it.id,
                            title = it.snippet.title,
                            description = it.snippet.description,
                            duration = it.contentDetails.duration,
                            viewCount = it.statistics.viewCount,
                            likeCount = it.statistics.likeCount,
                            disLikeCount = it.statistics.dislikeCount,
                            authorChannelTitle = it.snippet.channelTitle,
                            thumbnail = it.snippet.thumbnails.default.url
                        )
                    }
                    mVideos.addAll(videoItems)
                    mVideosLoader?.onResult(videoItems)
                    mVideoLoadingCanceler = null
                }

                override fun onError(error: Exception) {
                    mVideosLoader?.onError(error)
                }
            })
    }

    companion object {
        private const val LOG_TAG: String = "YouTubeViewModel"
    }
}