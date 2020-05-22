package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.utilities.Authorization
import com.yurii.youtubemusic.utilities.Preferences
import java.lang.Exception
import java.lang.IllegalStateException

interface VideosLoader {
    fun onResult(newVideos: List<VideoItem>, isLast: Boolean)
    fun onError(error: Exception)
}

class YouTubeMusicViewModel(application: Application, private val youTubeService: IYouTubeService) : AndroidViewModel(application) {
    private val context: Context = getApplication<Application>().baseContext

    var videosLoader: VideosLoader? = null

    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    val selectedPlaylist: LiveData<Playlist?>
        get() = _selectedPlayList

    private var videoLoadingCanceler: ICanceler? = null
    private val videos: MutableList<VideoItem> = mutableListOf()
    private var nextPageToken: String? = null

    init {
        Authorization.getGoogleCredentials(context)?.let {
            youTubeService.setCredentials(it)
        } ?: throw IllegalStateException("Cannot get Google account credentials")

        _selectedPlayList.value = Preferences.getSelectedPlayList(context)
        if (_selectedPlayList.value != null)
            loadVideos(null)
    }

    fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String? = null) {
        Log.i(LOG_TAG, "Start loading playLists with next page token $nextPageToken")
        youTubeService.loadPlayLists(observer, nextPageToken)
    }

    fun setNewPlayList(playlist: Playlist) {
        if (playlist != _selectedPlayList.value) {
            Preferences.setSelectedPlayList(context, playlist)
            _selectedPlayList.value = playlist
            videos.clear()
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

    fun getCurrentVideos() = videos
    fun isLast() = nextPageToken.isNullOrEmpty()

    private fun retrieveVideos(playlistItemListResponse: PlaylistItemListResponse) {
        youTubeService.loadVideosDetails(
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
                    videos.addAll(videoItems)
                    videosLoader?.onResult(videoItems, isLast())
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