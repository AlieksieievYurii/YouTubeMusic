package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse
import com.yurii.youtubemusic.GoogleAccount
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.services.youtube.YouTubeService
import com.yurii.youtubemusic.utilities.*
import com.yurii.youtubemusic.videoslist.VideoItemInterface
import java.io.File
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

class YouTubeMusicViewModel(application: Application, googleSignInAccount: GoogleSignInAccount) : AndroidViewModel(application) {
    private val youTubeService: IYouTubeService
    private val context: Context = getApplication<Application>().baseContext
    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    val selectedPlaylist: LiveData<Playlist?> get() = _selectedPlayList
    private var videoLoadingCanceler: ICanceler? = null
    private var nextPageToken: String? = null
    private val musicFolderStorage: File = DataStorage.getMusicStorage(context)

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
                videoItem.also {
                    addTag(videoItem)
                    videoItemChange?.onDownloadingFinished(videoItem)
                }
            }
        }
    }

    private fun addTag(videoItem: VideoItem) {
        val file = DataStorage.getMusic(context, videoItem)
        val tag = Tag(title = videoItem.title, authorChannel = videoItem.authorChannelTitle)
        TaggerV1(file).writeTag(tag)
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

    //TODO I think .mp3 should be removed from all files
    fun exists(videoItem: VideoItem): Boolean = File(musicFolderStorage, "${videoItem.videoId}.mp3").exists()

    fun isVideoItemLoading(videoItem: VideoItem): Boolean = MusicDownloaderService.Instance.serviceInterface?.isLoading(videoItem) ?: false

    fun getCurrentProgress(videoItem: VideoItem) = MusicDownloaderService.Instance.serviceInterface?.getProgress(videoItem)

    fun startDownloadMusic(videoItem: VideoItem) {
        context.startService(Intent(context, MusicDownloaderService::class.java).also {
            it.putExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM, videoItem)
        })
    }

    fun stopDownloading(videoItem: VideoItem) {
        MusicDownloaderService.Instance.serviceInterface?.cancel(videoItem)
    }

    fun removeVideoItem(videoItem: VideoItem) {
        val file = DataStorage.getMusic(context, videoItem)
        if (!file.delete())
            throw RuntimeException("Cannot remove the music file $file")
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


    inner class VideoItemProvider : VideoItemInterface {
        override fun download(videoItem: VideoItem) = this@YouTubeMusicViewModel.startDownloadMusic(videoItem)

        override fun cancelDownload(videoItem: VideoItem) = this@YouTubeMusicViewModel.stopDownloading(videoItem)

        override fun remove(videoItem: VideoItem) = this@YouTubeMusicViewModel.removeVideoItem(videoItem)

        override fun exists(videoItem: VideoItem): Boolean = this@YouTubeMusicViewModel.exists(videoItem)

        override fun isLoading(videoItem: VideoItem): Boolean = this@YouTubeMusicViewModel.isVideoItemLoading(videoItem)

        override fun getCurrentProgress(videoItem: VideoItem): Progress? = this@YouTubeMusicViewModel.getCurrentProgress(videoItem)
    }

    companion object {
        private const val LOG_TAG: String = "YouTubeViewModel"
    }
}