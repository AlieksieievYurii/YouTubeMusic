package com.youtubemusic.core.data.repository

import com.youtubemusic.core.data.toMediaItem
import com.youtubemusic.core.database.models.MediaItemEntity
import com.youtubemusic.core.model.MediaItem
import java.util.UUID

data class MediaItemCore(
    val mediaItem: MediaItem,
    val position: Int,
    val thumbnailUrl: String,
    val downloadingJobUUID: UUID?
)

fun List<MediaItemEntity>.toMediaItemCores() = map { it.toMediaItemCore() }

fun MediaItemEntity.toMediaItemCore() = MediaItemCore(
    mediaItem = toMediaItem(),
    position = position,
    downloadingJobUUID = downloadingJobId,
    thumbnailUrl = thumbnailUrl
)