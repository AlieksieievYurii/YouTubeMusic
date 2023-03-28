package com.yurii.youtubemusic.db

import androidx.room.*
import java.io.File
import java.util.UUID

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val mediaItemId: String,
    val title: String,
    val author: String,
    @ColumnInfo("duration") val durationInMillis: Long,
    val thumbnail: File,
    val mediaFile: File,
    val position: Int,
    val thumbnailUrl: String,
    val downloadingJobId: UUID?
)


@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
)


@Entity(tableName = "media_item_playlist_assignment", primaryKeys = ["mediaItemId", "playlistId"])
data class MediaItemPlayListAssignment(
    val mediaItemId: String,
    val playlistId: Long,
    val position: Int
)

data class PlaylistWithMediaItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "mediaItemId",
        associateBy = Junction(MediaItemPlayListAssignment::class)
    )
    val mediaItems: List<MediaItemEntity>
)

data class MediaItemWithPlaylists(
    @Embedded val mediaItemEntity: MediaItemEntity,
    @Relation(
        parentColumn = "mediaItemId",
        entityColumn = "playlistId",
        associateBy = Junction(MediaItemPlayListAssignment::class)
    )
    val playlists: List<PlaylistEntity>
)

@Entity(tableName = "you_tube_playlist_synchronization")
data class YouTubePlaylistSyncEntity(
    @PrimaryKey val youTubePlaylistId: String,
    val youTubePlaylistName: String,
    val thumbnailUrl: String
)

@Entity(tableName = "you_tube_playlist_synchronization_ref_to_media_playlist", primaryKeys = ["youTubePlaylistId", "playlistId"])
data class YouTubePlaylistSyncCrossRefToMediaPlaylist(
    val youTubePlaylistId: String,
    val playlistId: Long
)

data class YouTubePlaylistWithBoundMediaPlaylists(
    @Embedded val youTubePlaylistSync: YouTubePlaylistSyncEntity,
    @Relation(
        parentColumn = "youTubePlaylistId",
        entityColumn = "playlistId",
        associateBy = Junction(YouTubePlaylistSyncCrossRefToMediaPlaylist::class)
    )
    val playlists: List<PlaylistEntity>
)