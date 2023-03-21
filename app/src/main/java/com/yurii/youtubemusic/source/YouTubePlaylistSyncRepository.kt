package com.yurii.youtubemusic.source

import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.db.YouTubePlaylistSyncEntity
import com.yurii.youtubemusic.db.YouTubePlaylistSynchronizationDao
import com.yurii.youtubemusic.db.YouTubePlaylistSyncCrossRefToMediaPlaylist
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.models.toMediaItemPlaylists
import com.yurii.youtubemusic.utilities.mapItems
import javax.inject.Inject

class YouTubePlaylistSyncRepository @Inject constructor(private val youTubePlaylistSyncDao: YouTubePlaylistSynchronizationDao) {

    val youTubePlaylistSyncs = youTubePlaylistSyncDao.getYouTubePlaylistsSyncWithBoundedMediaPlaylists().mapItems {
        YouTubePlaylistSync(
            it.youTubePlaylistSync.youTubePlaylistId,
            it.youTubePlaylistSync.youTubePlaylistName,
            it.playlists.toMediaItemPlaylists()
        )
    }

    suspend fun addYouTubePlaylistSynchronization(youTubePlaylist: Playlist, playlistBinds: List<MediaItemPlaylist>) {
        youTubePlaylistSyncDao.insert(YouTubePlaylistSyncEntity(youTubePlaylist.id, youTubePlaylist.snippet.title))
        youTubePlaylistSyncDao.insertMediaItemPlaylistBinds(*playlistBinds.map {
            YouTubePlaylistSyncCrossRefToMediaPlaylist(youTubePlaylist.id, it.id)
        }.toTypedArray())
    }
}