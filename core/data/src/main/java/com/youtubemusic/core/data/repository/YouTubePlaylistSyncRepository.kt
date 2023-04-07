package com.youtubemusic.core.data.repository

import com.youtubemusic.core.common.mapItems
import com.youtubemusic.core.data.toMediaItemPlaylists
import com.youtubemusic.core.database.dao.YouTubePlaylistSynchronizationDao
import com.youtubemusic.core.database.models.YouTubePlaylistSyncEntity
import com.youtubemusic.core.database.models.YouTubePlaylistSyncToAppPlaylistCrossRef
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.YouTubePlaylist
import com.youtubemusic.core.model.YouTubePlaylistSync
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

    suspend fun addYouTubePlaylistSynchronization(youTubePlaylist: YouTubePlaylist, playlistBinds: List<MediaItemPlaylist>) {
        youTubePlaylistSyncDao.insert(YouTubePlaylistSyncEntity(youTubePlaylist.id, youTubePlaylist.name, youTubePlaylist.thumbnailUrl))
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
            YouTubePlaylistSyncToAppPlaylistCrossRef(youTubePlaylistId, it.id)
        }.toTypedArray())
    }
}