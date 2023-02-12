package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.db.MediaItemPlayListAssignment
import com.yurii.youtubemusic.db.PlaylistDao
import com.yurii.youtubemusic.db.PlaylistEntity
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.toMediaItemPlaylists
import com.yurii.youtubemusic.models.toMediaItems
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class PlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {
    private val mutex = Mutex()

    suspend fun assignMediaItemToPlaylists(mediaItemId: String, playlists: List<MediaItemPlaylist>) = mutex.withLock {
        val mediaItemPlaylists = playlists.map {
            MediaItemPlayListAssignment(
                mediaItemId,
                it.id,
                playlistDao.getAvailablePosition(it.id) ?: 0
            )
        }

        playlistDao.insertMediaItemPlaylist(*mediaItemPlaylists.toTypedArray())
    }

    suspend fun getAllPlaylists(): List<MediaItemPlaylist> {
        return playlistDao.getAllPlaylists().toMediaItemPlaylists()
    }

    suspend fun getMediaItemsFor(mediaItemPlaylist: MediaItemPlaylist): List<MediaItem> {
        return playlistDao.getMediaItemsForPlaylist(mediaItemPlaylist.id).toMediaItems()
    }

    suspend fun addPlaylist(name: String): Long {
        return playlistDao.insert(PlaylistEntity(name = name))
    }

    suspend fun changePositionInPlaylist(mediaItemPlaylist: MediaItemPlaylist, mediaItem: MediaItem, from: Int, to: Int) {
        playlistDao.changePosition(mediaItem.id, mediaItemPlaylist.id, from, to)
    }
}