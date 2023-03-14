package com.yurii.youtubemusic.screens.manager

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class PlaylistSyncBind(val playlistName: String)
data class DownloadingJob(val videoItemName: String, val videoItemId: String, val thumbnail: String)

@HiltViewModel
class DownloadManagerViewModel @Inject constructor() : ViewModel() {
}