package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.db.YouTubePlaylistSyncEntity
import com.yurii.youtubemusic.db.YouTubePlaylistSynchronizationDao
import com.yurii.youtubemusic.db.YouTubePlaylistSyncCrossRefToMediaPlaylist
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.models.toMediaItemPlaylists
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.yurii.youtubemusic.utilities.mapItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class YouTubePlaylistSyncRepository @Inject constructor(private val youTubePlaylistSyncDao: YouTubePlaylistSynchronizationDao) {

    val youTubePlaylistSyncs = youTubePlaylistSyncDao.getYouTubePlaylistsSyncWithBoundedMediaPlaylists().mapItems {
        YouTubePlaylistSync(
            it.youTubePlaylistSync.youTubePlaylistId,
            it.youTubePlaylistSync.youTubePlaylistName,
            it.youTubePlaylistSync.thumbnailUrl,
            it.playlists.toMediaItemPlaylists()
        )
    }

    suspend fun addYouTubePlaylistSynchronization(youTubePlaylist: Playlist, playlistBinds: List<MediaItemPlaylist>) {
        youTubePlaylistSyncDao.insert(YouTubePlaylistSyncEntity(youTubePlaylist.id, youTubePlaylist.name, youTubePlaylist.thumbnail))
        assignAppPlaylists(youTubePlaylist.id, playlistBinds)
    }

    suspend fun reassignPlaylists(youTubePlaylistId: String, playlists: List<MediaItemPlaylist>) = withContext(Dispatchers.IO) {
        youTubePlaylistSyncDao.deleteAppPlaylistsAssignments(youTubePlaylistId)
        assignAppPlaylists(youTubePlaylistId, playlists)
    }

    suspend fun removeYouTubePlaylistSynchronization(youTubePlaylistId: String) {
        youTubePlaylistSyncDao.deleteYouTubePlaylistSyncAndItsRelations(youTubePlaylistId)
    }

    private suspend fun assignAppPlaylists(youTubePlaylistId: String, playlists: List<MediaItemPlaylist>) {
        youTubePlaylistSyncDao.insertMediaItemPlaylistBinds(*playlists.map {
            YouTubePlaylistSyncCrossRefToMediaPlaylist(youTubePlaylistId, it.id)
        }.toTypedArray())
    }
}