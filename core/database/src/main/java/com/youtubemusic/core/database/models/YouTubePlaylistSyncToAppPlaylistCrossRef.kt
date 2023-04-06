package com.youtubemusic.core.database.models

import androidx.room.Entity

@Entity(tableName = "you_tube_playlist_synchronization_ref_to_media_playlist", primaryKeys = ["youTubePlaylistId", "playlistId"])
data class YouTubePlaylistSyncToAppPlaylistCrossRef(
    val youTubePlaylistId: String,
    val playlistId: Long
)
