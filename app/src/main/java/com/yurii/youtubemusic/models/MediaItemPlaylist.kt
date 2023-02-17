package com.yurii.youtubemusic.models

import android.os.Parcelable
import com.yurii.youtubemusic.db.PlaylistEntity
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaItemPlaylist(val id: Long, val name: String) : Parcelable {
    companion object {
        val ALL = MediaItemPlaylist(-1, "All")
    }
}

fun MediaItemPlaylist.toPlaylistEntity() = PlaylistEntity(id, name)

fun List<PlaylistEntity>.toMediaItemPlaylists(): List<MediaItemPlaylist> {
    return map { MediaItemPlaylist(it.playlistId, it.name) }
}