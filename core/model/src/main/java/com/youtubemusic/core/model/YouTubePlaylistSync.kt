package com.youtubemusic.core.model

data class YouTubePlaylistSync(
    val youTubePlaylistId: String,
    val youTubePlaylistName: String,
    val thumbnailUrl: String,
    val mediaItemPlaylists: List<MediaItemPlaylist>
)