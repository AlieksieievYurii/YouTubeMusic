package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.media.MediaStorage
import javax.inject.Inject

class MediaCreator @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaStorage: MediaStorage
) {

    suspend fun createMediaItem(videoItem: VideoItem, playlists: List<MediaItemPlaylist>) {
        val createdMediaItem = registerMediaItem(videoItem)
        playlistRepository.assignMediaItemToPlaylists(createdMediaItem, playlists)
    }

    private suspend fun registerMediaItem(videoItem: VideoItem): MediaItem {
        val mediaFile = mediaStorage.getMediaFile(videoItem)
        val thumbnailFile = mediaStorage.getThumbnail(videoItem)

        if (!mediaFile.exists())
            throw IllegalStateException("Media file does not exist")

        if (!thumbnailFile.exists())
            throw IllegalStateException("Thumbnail file does not exist")

        val mediaItem = MediaItem(
            id = videoItem.id,
            title = videoItem.title,
            author = videoItem.author,
            durationInMillis = videoItem.durationInMillis,
            thumbnail = thumbnailFile,
            mediaFile = mediaFile,
            description = ""
        )

        mediaRepository.addMediaItem(mediaItem)

        return mediaItem
    }
}