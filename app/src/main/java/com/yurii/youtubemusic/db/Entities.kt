package com.yurii.youtubemusic.db

import androidx.room.*
import com.yurii.youtubemusic.models.MediaItem
import java.io.File

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val mediaItemId: String,
    val title: String,
    val author: String,
    @ColumnInfo("duration") val durationInMillis: Long,
    val thumbnail: File,
    val mediaFile: File,
    val position: Int
)


@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val name: String,
)


@Entity(tableName = "media_item_playlist_assignment", primaryKeys = ["mediaItemId", "playlistId"])
data class MediaItemPlayListAssignment(
    val mediaItemId: String,
    val playlistId: Int,
    @ColumnInfo() val position: Int
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

fun MediaItem.toEntity(position: Int): MediaItemEntity = MediaItemEntity(
    mediaItemId = id,
    title = title,
    author = author,
    durationInMillis = durationInMillis,
    thumbnail = thumbnail,
    mediaFile = mediaFile,
    position = position
)