package com.youtubemusic.core.model


data class YouTubePlaylist(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val videoCount: Long
)