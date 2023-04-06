package com.youtubemusic.core.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "you_tube_playlist_synchronization")
data class YouTubePlaylistSyncEntity(
    @PrimaryKey val youTubePlaylistId: String,
    val youTubePlaylistName: String,
    val thumbnailUrl: String
)