package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.db.MediaItemPlayListAssignment
import com.yurii.youtubemusic.db.PlaylistDao
import com.yurii.youtubemusic.db.PlaylistEntity
import com.yurii.youtubemusic.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {
    private val mutex = Mutex()

    /**
     * Assigns given [playlists] for the given [mediaItem].
     * Note: It does not add new playlists, it reassigns the all.
     */
    suspend fun assignMediaItemToPlaylists(mediaItem: MediaItem, playlists: List<MediaItemPlaylist>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            playlistDao.removeAllAssignments(mediaItem.id)
            val mediaItemPlaylists = playlists.map {
                MediaItemPlayListAssignment(
                    mediaItem.id,
                    it.id,
                    playlistDao.getAvailablePosition(it.id) ?: 0
                )
            }

            playlistDao.insertMediaItemPlaylist(*mediaItemPlaylists.toTypedArray())
        }
    }

    suspend fun getAllPlaylists(): List<MediaItemPlaylist> = withContext(Dispatchers.IO) {
        playlistDao.getAllPlaylists().toMediaItemPlaylists()
    }

    suspend fun getAssignedPlaylistsFor(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        playlistDao.getAssignedPlaylists(mediaItem.id).toMediaItemPlaylists()
    }

    fun getPlaylistFlow(): Flow<List<MediaItemPlaylist>> = playlistDao.getPlaylistsFlow().map { it.toMediaItemPlaylists() }

    suspend fun renamePlaylist(mediaItemPlaylist: MediaItemPlaylist, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.update(mediaItemPlaylist.copy(name = newName).toPlaylistEntity())
    }

    suspend fun removePlaylist(mediaItemPlaylist: MediaItemPlaylist) = withContext(Dispatchers.IO) {
        playlistDao.delete(mediaItemPlaylist.toPlaylistEntity())
    }

    suspend fun getMediaItemsFor(mediaItemPlaylist: MediaItemPlaylist) = withContext(Dispatchers.IO) {
        playlistDao.getMediaItemsForPlaylist(mediaItemPlaylist.id).toMediaItems()
    }

    suspend fun addPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insert(PlaylistEntity(name = name))
    }

    suspend fun changePositionInPlaylist(mediaItemPlaylist: MediaItemPlaylist, mediaItem: MediaItem, from: Int, to: Int) =
        withContext(Dispatchers.IO) {
            playlistDao.changePosition(mediaItem.id, mediaItemPlaylist.id, from, to)
        }
}