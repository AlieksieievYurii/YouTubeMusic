package com.youtubemusic.core.database.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class YouTubePlaylistWithBoundMediaPlaylists(
    @Embedded val youTubePlaylistSync: YouTubePlaylistSyncEntity,
    @Relation(
        parentColumn = "youTubePlaylistId",
        entityColumn = "playlistId",
        associateBy = Junction(YouTubePlaylistSyncToAppPlaylistCrossRef::class)
    )
    val playlists: List<PlaylistEntity>
)