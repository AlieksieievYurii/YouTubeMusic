package com.yurii.youtubemusic.viewmodels.youtubefragment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.services.youtube.IYouTubeService
import com.yurii.youtubemusic.utilities.Authorization
import com.yurii.youtubemusic.utilities.Preferences
import java.lang.IllegalStateException

class YouTubeMusicViewModel(application: Application, private val youTubeService: IYouTubeService) : AndroidViewModel(application) {
    private val _selectedPlayList: MutableLiveData<Playlist?> = MutableLiveData()
    val selectedPlaylist: LiveData<Playlist?>
        get() = _selectedPlayList

    init {
        Log.i("ViewModel", "Init")
        Authorization.getGoogleCredentials(getApplication<Application>().baseContext)?.let {
            youTubeService.setCredentials(it)
        } ?: throw IllegalStateException("Cannot get Google account credentials")
        _selectedPlayList.value = Preferences.getSelectedPlayList(getApplication<Application>().baseContext)
    }
}