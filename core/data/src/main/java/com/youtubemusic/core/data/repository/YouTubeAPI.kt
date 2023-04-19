package com.youtubemusic.core.data.repository

import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.VideoListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class YouTubeAPI @Inject constructor(
    private val transport: HttpTransport,
    private val jsonFactory: JsonFactory,
    private val googleAccount: GoogleAccount
) {
    private val service: YouTube by lazy {
        YouTube.Builder(transport, jsonFactory, googleAccount.getGoogleAccountCredentialUsingOAuth2()).build()
    }

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

    suspend fun getVideos(query: String, pageToken: String?): SearchListResponse = withContext(Dispatchers.IO) {
        service.search()
            .list("snippet")
            .setOrder("viewCount")
            .setMaxResults(10)
            .setQ(query)
            .setType("video")
            .setPageToken(pageToken)
            .execute()
    }

    suspend fun getVideosDetails(ids: List<String>): VideoListResponse = withContext(Dispatchers.IO) {
        service.videos().list("snippet,statistics,contentDetails")
            .setId(ids.joinToString(","))
            .execute()
    }

    suspend fun getPlaylistDetails(playlistId: String): PlaylistListResponse = withContext(Dispatchers.IO) {
        service.playlists().list("contentDetails,snippet,status").setId(playlistId).execute()
    }
}