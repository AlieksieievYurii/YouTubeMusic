package com.yurii.youtubemusic.models

import com.yurii.youtubemusic.db.PlaylistEntity

data class MediaItemPlaylist(val id: Long, val name: String)

fun MediaItemPlaylist.toPlaylistEntity() = PlaylistEntity(id, name)

fun List<PlaylistEntity>.toMediaItemPlaylists(): List<MediaItemPlaylist> {
    return map { MediaItemPlaylist(it.playlistId, it.name) }
}