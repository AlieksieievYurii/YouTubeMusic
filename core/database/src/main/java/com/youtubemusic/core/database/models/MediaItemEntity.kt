package com.youtubemusic.core.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.util.*

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