package com.youtubemusic.core.database.models

import androidx.room.Entity

@Entity(tableName = "media_item_playlist_assignment", primaryKeys = ["mediaItemId", "playlistId"])
data class MediaItemPlayListAssignment(
    val mediaItemId: String,
    val playlistId: Long,
    val position: Int
)