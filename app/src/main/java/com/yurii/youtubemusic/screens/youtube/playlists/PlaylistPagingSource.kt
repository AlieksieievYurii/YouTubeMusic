package com.yurii.youtubemusic.screens.youtube.playlists

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import com.yurii.youtubemusic.utilities.EmptyListException
import java.lang.Exception

class PlaylistPagingSource(private val youTubeAPI: YouTubeAPI) : PagingSource<String, Playlist>() {
    override fun getRefreshKey(state: PagingState<String, Playlist>): String? {
        TODO("Not yet implemented")
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Playlist> {
        try {
            val playlists = youTubeAPI.getMyPlaylists(pageToken = params.key, maxResult = params.loadSize.toLong())

            if (playlists.items.isEmpty())
                return LoadResult.Error(EmptyListException())

            return LoadResult.Page(
                data = playlists.items,
                nextKey = playlists.nextPageToken,
                prevKey = playlists.prevPageToken
            )
        } catch (error: Exception) {
            return LoadResult.Error(error)
        }
    }
}