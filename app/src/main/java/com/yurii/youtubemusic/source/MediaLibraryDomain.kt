package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.isDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryDomain @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository
) {

    private val _itemDeleted = MutableSharedFlow<Item>()
    val itemDeleted = _itemDeleted.asSharedFlow()

    fun getMediaItems(playlist: MediaItemPlaylist): Flow<List<MediaItem>> {
        return if (playlist.isDefault())
            mediaRepository.getOrderedMediaItems()
        else
            playlistRepository.getMediaItemsFor(playlist)
    }

    suspend fun changePosition(playlist: MediaItemPlaylist, mediaItem: MediaItem, from: Int, to: Int) {
        if (playlist.isDefault())
            mediaRepository.changePosition(mediaItem, from, to)
        else
            playlistRepository.changePositionInPlaylist(playlist, mediaItem, from, to)
    }

    suspend fun deleteMediaItem(item: Item) {
        playlistRepository.removeItemFromPlaylists(item)
        mediaRepository.delete(item)
        _itemDeleted.emit(item)
    }
}