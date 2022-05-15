package com.yurii.youtubemusic.screens.youtube

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("BlockingMethodInNonBlockingContext")
class YouTubeAPI(credential: GoogleAccountCredential) {
    private val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    private val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    private var service: YouTube = YouTube.Builder(transport, jsonFactory, credential).build()

    suspend fun getMyPlaylists(pageToken: String? = null, maxResult: Long = 10): PlaylistListResponse =
        withContext(Dispatchers.IO) {
            service.playlists().list("snippet,contentDetails")
                .setMine(true)
                .setPageToken(pageToken)
                .setMaxResults(maxResult)
                .execute()
        }

    suspend fun getItemsFromPlaylist(playlistId: String, pageToken: String? = null, maxResult: Long = 10): PlaylistItemListResponse =
        withContext(Dispatchers.IO) {
            service.playlistItems().list("snippet")
                .setPlaylistId(playlistId)
                .setMaxResults(maxResult)
                .setPageToken(pageToken)
                .execute()
        }


    suspend fun getVideosDetails(ids: List<String>): VideoListResponse = withContext(Dispatchers.IO) {
        service.videos().list("snippet,statistics,contentDetails")
            .setId(ids.joinToString(","))
            .execute()
    }
}