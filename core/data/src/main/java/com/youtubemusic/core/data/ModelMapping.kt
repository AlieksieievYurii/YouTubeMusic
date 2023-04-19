package com.youtubemusic.core.data

import com.google.api.services.youtube.model.PlaylistListResponse
import com.youtubemusic.core.database.models.MediaItemEntity
import com.youtubemusic.core.database.models.PlaylistEntity
import com.youtubemusic.core.model.MediaItem
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.YouTubePlaylistDetails
import com.youtubemusic.core.model.YouTubePlaylistPrivacyStatus

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

fun PlaylistListResponse.toYouTubePlaylistDetails(): YouTubePlaylistDetails {
    val playlist = items.first()
    return YouTubePlaylistDetails(
        id = playlist.id,
        title = playlist.snippet.title,
        videosNumber = playlist.contentDetails.itemCount,
        thumbnailUrl = playlist.snippet.thumbnails.maxres?.url,
        channelTitle = playlist.snippet.channelTitle,
        privacyStatus = when (playlist.status.privacyStatus) {
            "public" -> YouTubePlaylistPrivacyStatus.PUBLIC
            "unlisted" -> YouTubePlaylistPrivacyStatus.UNLISTED
            "private" -> YouTubePlaylistPrivacyStatus.PRIVATE
            else -> throw IllegalStateException("Unknown privacyStatus: ${playlist.status.privacyStatus}")
        }
    )
}