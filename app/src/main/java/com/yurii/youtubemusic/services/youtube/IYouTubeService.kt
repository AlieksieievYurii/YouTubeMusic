package com.yurii.youtubemusic.services.youtube

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.youtube.model.*
import java.lang.Exception

interface ICanceler {
    fun cancel()
}

/**
 * This is callback for [IYouTubeService]
 */
interface YouTubeObserver<T> {
    fun onResult(result: T)
    fun onError(error: Exception)
}

/**
 * Interface for implementation of YouTube service
 */
interface IYouTubeService {
    fun setCredentials(credential: GoogleAccountCredential)
    fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String? = null): ICanceler
    fun loadPlayListItems(playlistId: String, observer: YouTubeObserver<PlaylistItemListResponse>, nextPageToken: String? = null): ICanceler
    fun loadVideosDetails(ids: List<String>, observer: YouTubeObserver<VideoListResponse>, nextPageToken: String? = null): ICanceler
}