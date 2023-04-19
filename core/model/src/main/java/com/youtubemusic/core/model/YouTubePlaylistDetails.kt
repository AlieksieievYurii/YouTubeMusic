package com.youtubemusic.core.model

enum class YouTubePlaylistPrivacyStatus {
    PUBLIC, UNLISTED, PRIVATE
}

data class YouTubePlaylistDetails(
    val id: String,
    val title: String,
    val videosNumber: Long,
    val thumbnailUrl: String?,
    val channelTitle: String,
    val privacyStatus: YouTubePlaylistPrivacyStatus
)