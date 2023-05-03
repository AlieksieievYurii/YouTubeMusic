package com.youtubemusic.core.model

import java.util.UUID

data class MediaItemCore(
    val mediaItem: MediaItem,
    val position: Int,
    val thumbnailUrl: String,
    val downloadingJobUUID: UUID?
)