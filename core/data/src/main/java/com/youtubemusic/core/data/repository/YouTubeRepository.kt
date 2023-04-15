package com.youtubemusic.core.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.Video
import com.youtubemusic.core.data.AllYouTubePlaylistsSynchronized
import com.youtubemusic.core.data.EmptyListException
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.core.model.YouTubePlaylist
import kotlinx.coroutines.flow.first
import org.threeten.bp.Duration
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor(
    private val youTubeAPI: YouTubeAPI,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository
) {

    suspend fun getAllMyPlaylistsIds(): List<String> {
        val ids = mutableSetOf<String>()
        var nextPage: String? = null
        do {
            val res = youTubeAPI.getMyPlaylists(nextPage)
            ids.addAll(res.items.map { it.id })
            nextPage = res.nextPageToken

        } while (nextPage != null)

        return ids.toList()
    }

    suspend fun getAllVideoItemsFromPlaylist(playlistId: String): List<VideoItem> {
        val videoItems = mutableListOf<VideoItem>()
        var nextPage: String? = null
        do {
            val res = youTubeAPI.getItemsFromPlaylist(playlistId, nextPage)
            nextPage = res.nextPageToken
            if (res.items.isNotEmpty()) {
                val videos = youTubeAPI.getVideosDetails(res.items.map { it.snippet.resourceId.videoId }).items
                videoItems.addAll(videos.map { it.toVideoItem() })
            }
        } while (nextPage != null)

        return videoItems
    }

    fun getYouTubePlaylistsPagingSource(): PlaylistsPagingSource {
        return PlaylistsPagingSource(youTubeAPI)
    }

    fun getYouTubeVideosPagingSource(query: String): YouTubeVideosPagingSource {
        return YouTubeVideosPagingSource(youTubeAPI, query)
    }

    fun getYouTubePlaylistVideosPagingSource(playlist: YouTubePlaylist): YouTubePlaylistVideosPagingSource {
        return YouTubePlaylistVideosPagingSource(youTubeAPI, playlist.id)
    }

    fun getExcludingAlreadySyncPlaylistPagingSource(): ExcludingAlreadySyncPlaylistPagingSource {
        return ExcludingAlreadySyncPlaylistPagingSource(youTubeAPI, youTubePlaylistSyncRepository)
    }
}

open class PlaylistsPagingSource(private val youTubeAPI: YouTubeAPI) : PagingSource<String, YouTubePlaylist>() {
    override fun getRefreshKey(state: PagingState<String, YouTubePlaylist>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, YouTubePlaylist> {
        try {
            val playlists = youTubeAPI.getMyPlaylists(pageToken = params.key, maxResult = params.loadSize.toLong())

            if (playlists.items.isEmpty())
                return LoadResult.Error(EmptyListException())

            return LoadResult.Page(
                data = playlists.items.map { it.toYouTubePlaylist() },
                nextKey = playlists.nextPageToken,
                prevKey = playlists.prevPageToken
            )
        } catch (error: Exception) {
            return LoadResult.Error(error)
        }
    }
}

class ExcludingAlreadySyncPlaylistPagingSource(
    youTubeAPI: YouTubeAPI,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository
) : PlaylistsPagingSource(youTubeAPI) {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, YouTubePlaylist> {
        val result = super.load(params)

        if (result is LoadResult.Page)
            return excludeAlreadySyncedYouTubePlaylists(result)

        return result
    }

    private suspend fun excludeAlreadySyncedYouTubePlaylists(input: LoadResult.Page<String, YouTubePlaylist>): LoadResult<String, YouTubePlaylist> {
        val alreadySyncPlaylistsIds = youTubePlaylistSyncRepository.youTubePlaylistSyncs.first().map { it.youTubePlaylistId }

        val filteredData = input.data.filter { playlist ->
            !alreadySyncPlaylistsIds.contains(playlist.id)
        }

        if (filteredData.isEmpty())
            return LoadResult.Error(AllYouTubePlaylistsSynchronized())

        return input.copy(data = filteredData)
    }
}

class YouTubeVideosPagingSource(private val youTubeAPI: YouTubeAPI, private val query: String) : PagingSource<String, VideoItem>() {
    override fun getRefreshKey(state: PagingState<String, VideoItem>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, VideoItem> {
        try {
            val results = youTubeAPI.getVideos(query, pageToken = params.key)

            if (results.items.isEmpty())
                return LoadResult.Error(EmptyListException())

            val videos = youTubeAPI.getVideosDetails(ids = results.items.map { it.id.videoId })

            return LoadResult.Page(
                data = videos.items.map { it.toVideoItem() },
                prevKey = results.prevPageToken,
                nextKey = results.nextPageToken
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}

class YouTubePlaylistVideosPagingSource(private val youTubeAPI: YouTubeAPI, private val playlistId: String) : PagingSource<String, VideoItem>() {
    override fun getRefreshKey(state: PagingState<String, VideoItem>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, VideoItem> {
        try {
            val results = youTubeAPI.getItemsFromPlaylist(playlistId, pageToken = params.key, maxResult = params.loadSize.toLong())

            if (results.items.isEmpty())
                return LoadResult.Error(EmptyListException())

            val videos = youTubeAPI.getVideosDetails(ids = results.items.map { it.snippet.resourceId.videoId })

            return LoadResult.Page(
                data = videos.items.map { it.toVideoItem() },
                prevKey = results.prevPageToken,
                nextKey = results.nextPageToken
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}

fun Playlist.toYouTubePlaylist() = YouTubePlaylist(
    this.id,
    this.snippet.title,
    this.snippet.thumbnails.default.url,
    this.contentDetails.itemCount
)

fun Video.toVideoItem() = VideoItem(
    id = id,
    title = snippet.title,
    author = snippet.channelTitle,
    durationInMillis = Duration.parse(contentDetails.duration).toMillis(),
    description = snippet.description,
    viewCount = statistics.viewCount,
    likeCount = statistics.likeCount,
    thumbnail = snippet.thumbnails.default.url,
    normalThumbnail = snippet.thumbnails.medium.url
)