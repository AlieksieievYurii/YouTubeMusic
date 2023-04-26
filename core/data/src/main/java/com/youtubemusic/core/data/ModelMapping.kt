package com.youtubemusic.core.data

import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.Video
import com.youtubemusic.core.database.models.MediaItemEntity
import com.youtubemusic.core.database.models.PlaylistEntity
import com.youtubemusic.core.model.*
import org.threeten.bp.Duration
import java.math.BigInteger
import java.util.*

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

fun OrderEnum.toQueryKey() = when (this) {
    OrderEnum.RELEVANCE -> "relevance"
    OrderEnum.UPLOAD_DATE -> "date"
    OrderEnum.BY_TITLE -> "title"
    OrderEnum.VIEW_COUNT -> "viewCount"
    OrderEnum.RATING -> "rating"
}

fun DurationEnum.toQueryKey() = when(this) {
    DurationEnum.ANY -> "any"
    DurationEnum.SHORT -> "short"
    DurationEnum.MEDIUM -> "medium"
    DurationEnum.LONG -> "long"
}

fun Playlist.toYouTubePlaylist() = YouTubePlaylist(
    this.id,
    this.snippet.title,
    this.snippet.thumbnails.default.url,
    this.contentDetails.itemCount
)

fun Video.toVideoItem() = VideoItem(
    id = id,
    title = snippet.title,
    author = snippet.channelTitle,
    durationInMillis = Duration.parse(contentDetails.duration).toMillis(),
    description = snippet.description,
    viewCount = statistics.viewCount ?: BigInteger.ZERO,
    likeCount = statistics.likeCount ?: BigInteger.ZERO,
    thumbnail = snippet.thumbnails.default.url,
    normalThumbnail = snippet.thumbnails.medium.url,
    publishDate = Date(snippet.publishedAt.value)
)