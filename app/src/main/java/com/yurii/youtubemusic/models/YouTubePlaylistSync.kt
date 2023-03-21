package com.yurii.youtubemusic.models

data class YouTubePlaylistSync(
    val youTubePlaylistId: String,
    val youTubePlaylistName: String,
    val mediaItemPlaylists: List<MediaItemPlaylist>
)