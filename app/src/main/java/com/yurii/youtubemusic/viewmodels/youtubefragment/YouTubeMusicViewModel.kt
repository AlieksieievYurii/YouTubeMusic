package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.utilities.Authorization
import com.yurii.youtubemusic.utilities.Preferences
import java.lang.IllegalStateException

class YouTubeMusicViewModel(application: Application, private val youTubeService: IYouTubeService) : AndroidViewModel(application) {
    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    private val context: Context = getApplication<Application>().baseContext
    val selectedPlaylist: LiveData<Playlist?>
        get() = _selectedPlayList

    init {
        Authorization.getGoogleCredentials(context)?.let {
            youTubeService.setCredentials(it)
        } ?: throw IllegalStateException("Cannot get Google account credentials")
        _selectedPlayList.value = Preferences.getSelectedPlayList(context)
    }

    fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String? = null) {
        Log.i(LOG_TAG, "Start loading playLists with next page token $nextPageToken")
        youTubeService.loadPlayLists(observer, nextPageToken)
    }

    fun setNewPlayList(playlist: Playlist) {
        if (playlist != _selectedPlayList.value) {
            Preferences.setSelectedPlayList(context, playlist)
            _selectedPlayList.value = playlist
        }
    }



    companion object {
        private const val LOG_TAG: String = "YouTubeViewModel"
    }
}