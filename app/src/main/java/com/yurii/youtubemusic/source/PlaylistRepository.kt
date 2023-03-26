package com.yurii.youtubemusic.source

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
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val playlistDao: PlaylistDao) {

    private val lock = Mutex()

    suspend fun assignMediaItemToPlaylists(mediaItem: MediaItem, newPlaylists: List<MediaItemPlaylist>) = lock.withLock {
        withContext(Dispatchers.IO) {
            val alreadyAssignedPlaylists = playlistDao.getAssignedPlaylists(mediaItem.id).toMediaItemPlaylists().toSet()
            val playlistsToAssign = newPlaylists subtract alreadyAssignedPlaylists
            val playlistsToRemove = alreadyAssignedPlaylists subtract newPlaylists.toSet()
            playlistsToAssign.forEach {
                val availablePosition = playlistDao.getAvailablePosition(it.id) ?: 0
                playlistDao.insertMediaItemPlaylist(MediaItemPlayListAssignment(mediaItem.id, it.id, availablePosition))
            }
            playlistsToRemove.forEach { playlistDao.detachPlaylist(mediaItem.id, it.id) }
        }
    }

    suspend fun assignDownloadingMediaItemToPlaylists(mediaItem: MediaItem, playlists: List<MediaItemPlaylist>) = lock.withLock {
        withContext(Dispatchers.IO) {
            playlists.forEach {
                playlistDao.insertMediaItemPlaylist(MediaItemPlayListAssignment(mediaItem.id, it.id, PlaylistDao.UNSPECIFIED_POSITION))
            }
        }
    }

    suspend fun getAssignedPlaylistsFor(itemId: String) = withContext(Dispatchers.IO) {
        playlistDao.getAssignedPlaylists(itemId).toMediaItemPlaylists()
    }

    fun getPlaylists(): Flow<List<MediaItemPlaylist>> = playlistDao.getPlaylists().map { it.toMediaItemPlaylists() }

    suspend fun renamePlaylist(mediaItemPlaylist: MediaItemPlaylist, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.update(mediaItemPlaylist.copy(name = newName).toPlaylistEntity())
    }

    suspend fun setMediaItemAsDownloaded(itemId: String) = lock.withLock {
        getAssignedPlaylistsFor(itemId).forEach {
            val availablePosition = playlistDao.getAvailablePosition(it.id) ?: 0
            playlistDao.setPosition(itemId, it.id, availablePosition)
        }
    }

    suspend fun removePlaylist(mediaItemPlaylist: MediaItemPlaylist) = lock.withLock {
        withContext(Dispatchers.IO) {
            playlistDao.removePlaylistAssignments(mediaItemPlaylist.id)
            playlistDao.delete(mediaItemPlaylist.toPlaylistEntity())
        }
    }

    fun getMediaItemsFor(mediaItemPlaylist: MediaItemPlaylist) =
        playlistDao.getMediaItemsForPlaylistFlow(mediaItemPlaylist.id).map { it.toMediaItems() }

    suspend fun addPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insert(PlaylistEntity(name = name))
    }

    suspend fun changePositionInPlaylist(mediaItemPlaylist: MediaItemPlaylist, mediaItem: MediaItem, from: Int, to: Int) =
        withContext(Dispatchers.IO) {
            playlistDao.changePosition(mediaItem.id, mediaItemPlaylist.id, from, to)
        }

    suspend fun removeItemFromPlaylists(item: Item) {
        withContext(Dispatchers.IO) {
            playlistDao.removeMediaItemFromPlaylists(item.id)
        }
    }
}