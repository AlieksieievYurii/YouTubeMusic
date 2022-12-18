package com.yurii.youtubemusic.screens.youtube.playlists

import com.google.api.services.youtube.model.Playlist as YouTubePlaylistModel

data class Playlist(
    val id: String,
    val name: String,
    val thumbnail: String,
    val videoCount: Long
)

fun YouTubePlaylistModel.toPlaylist(): Playlist =
    Playlist(
        this.id,
        this.snippet.title,
        this.snippet.thumbnails.default.url,
        this.contentDetails.itemCount
    )