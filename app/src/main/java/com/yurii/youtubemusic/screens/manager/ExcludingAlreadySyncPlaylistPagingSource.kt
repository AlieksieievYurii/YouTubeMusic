package com.yurii.youtubemusic.screens.manager

import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.yurii.youtubemusic.screens.youtube.playlists.PlaylistsPagingSource
import com.yurii.youtubemusic.source.YouTubePlaylistSyncRepository
import kotlinx.coroutines.flow.first

class AllYouTubePlaylistsSynchronized : Exception()

class ExcludingAlreadySyncPlaylistPagingSource(
    youTubeAPI: YouTubeAPI,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository
) : PlaylistsPagingSource(youTubeAPI) {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Playlist> {
        val result = super.load(params)

        if (result is LoadResult.Page)
            return excludeAlreadySyncedYouTubePlaylists(result)

        return result
    }

    private suspend fun excludeAlreadySyncedYouTubePlaylists(input: LoadResult.Page<String, Playlist>): LoadResult<String, Playlist> {
        val alreadySyncPlaylistsIds = youTubePlaylistSyncRepository.youTubePlaylistSyncs.first().map { it.youTubePlaylistId }

        val filteredData = input.data.filter { playlist ->
            !alreadySyncPlaylistsIds.contains(playlist.id)
        }

        if (filteredData.isEmpty())
            return LoadResult.Error(AllYouTubePlaylistsSynchronized())

        return input.copy(data = filteredData)
    }
}