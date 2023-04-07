package com.youtubemusic.core.data

import com.youtubemusic.core.database.models.MediaItemEntity
import com.youtubemusic.core.database.models.PlaylistEntity
import com.youtubemusic.core.model.MediaItem
import com.youtubemusic.core.model.MediaItemPlaylist

fun MediaItemEntity.toMediaItem() = MediaItem(
    id = mediaItemId,
    title = title,
    author = author,
    durationInMillis = durationInMillis,
    description = "",
    thumbnail = thumbnail,
    mediaFile = mediaFile
)

fun List<MediaItemEntity>.toMediaItems() = map { it.toMediaItem() }

fun MediaItemPlaylist.toPlaylistEntity() = PlaylistEntity(id, name)

fun List<PlaylistEntity>.toMediaItemPlaylists(): List<MediaItemPlaylist> {
    return map { MediaItemPlaylist(it.playlistId, it.name) }
}