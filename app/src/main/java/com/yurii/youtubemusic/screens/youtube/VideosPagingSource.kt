package com.yurii.youtubemusic.screens.youtube

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yurii.youtubemusic.screens.youtube.models.VideoItem

class VideosPagingSource(private val youTubeAPI: YouTubeAPI, private val playlistId: String) : PagingSource<String, VideoItem>() {
    override fun getRefreshKey(state: PagingState<String, VideoItem>): String? {
        TODO("Not yet implemented")
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, VideoItem> {
        return try {
            val results = youTubeAPI.getItemsFromPlaylist(playlistId, pageToken = params.key, maxResult = params.loadSize.toLong())
            val videos = youTubeAPI.getVideosDetails(ids = results.items.map { it.snippet.resourceId.videoId })

            LoadResult.Page(
                data = videos.items.map { VideoItem.createFrom(it) },
                prevKey = results.prevPageToken,
                nextKey = results.nextPageToken
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}