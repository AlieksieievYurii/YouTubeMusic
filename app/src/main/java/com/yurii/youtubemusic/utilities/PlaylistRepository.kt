package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.db.MediaItemPlayListAssignment
import com.yurii.youtubemusic.db.PlaylistDao
import com.yurii.youtubemusic.db.PlaylistEntity
import com.yurii.youtubemusic.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    fun getPlaylistFlow(): Flow<List<MediaItemPlaylist>> = playlistDao.getPlaylistsFlow().map { it.toMediaItemPlaylists() }

    suspend fun renamePlaylist(mediaItemPlaylist: MediaItemPlaylist, newName: String) {
        playlistDao.update(mediaItemPlaylist.copy(name = newName).toPlaylistEntity())
    }

    suspend fun removePlaylist(mediaItemPlaylist: MediaItemPlaylist) {
        playlistDao.delete(mediaItemPlaylist.toPlaylistEntity())
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