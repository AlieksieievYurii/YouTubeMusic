package com.youtubemusic.core.data.repository

import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.VideoListResponse
import com.youtubemusic.core.data.SearchFilterData
import com.youtubemusic.core.data.UploadDateEnum
import com.youtubemusic.core.data.toQueryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
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

    suspend fun getVideos(query: String, searchFilter: SearchFilterData, pageToken: String?): SearchListResponse =
        withContext(Dispatchers.IO) {
            val videoType = if (searchFilter.featureEpisode && searchFilter.featureMovie)
                "any"
            else if (searchFilter.featureMovie)
                "movie"
            else if (searchFilter.featureEpisode)
                "episode"
            else "any"

            val request = service.search()
                .list("snippet")
                .setOrder(searchFilter.orderBy.toQueryKey())
                .setVideoDuration(searchFilter.duration.toQueryKey())
                .setVideoSyndicated(if (searchFilter.featureSyndicated) "true" else "any")
                .setVideoType(videoType)
                .setMaxResults(10)
                .setQ(query)
                .setType("video")
                .setPageToken(pageToken)

            if (searchFilter.uploadDate != UploadDateEnum.ANYTIME) {
                val time = Calendar.getInstance()

                when (searchFilter.uploadDate) {
                    UploadDateEnum.LAST_HOUR -> time.add(Calendar.HOUR, -1)
                    UploadDateEnum.TODAY -> time.set(Calendar.HOUR, 0)
                    UploadDateEnum.THIS_WEEK -> time.set(Calendar.WEEK_OF_MONTH, 0)
                    UploadDateEnum.THIS_MONTH -> {
                        time.set(Calendar.HOUR, 0)
                        time.set(Calendar.DAY_OF_MONTH, 1)
                    }
                    UploadDateEnum.THIS_YEAR -> {
                        time.set(Calendar.HOUR, 0)
                        time.set(Calendar.DAY_OF_MONTH, 1)
                        time.set(Calendar.MONTH, 0)
                    }
                    else -> throw IllegalStateException("Unhandled UploadDate enum: ${searchFilter.uploadDate}")
                }

                request.publishedAfter = DateTime(time.time)
            }

            request.execute()
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