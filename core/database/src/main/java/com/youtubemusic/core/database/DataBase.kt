package com.youtubemusic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.youtubemusic.core.database.dao.MediaItemDao
import com.youtubemusic.core.database.dao.PlaylistDao
import com.youtubemusic.core.database.dao.YouTubePlaylistSynchronizationDao
import com.youtubemusic.core.database.models.*

@Database(
    entities = [MediaItemEntity::class,
        PlaylistEntity::class,
        MediaItemPlayListAssignment::class,
        YouTubePlaylistSyncEntity::class,
        YouTubePlaylistSyncToAppPlaylistCrossRef::class], version = 2
)
@TypeConverters(FileConverter::class, UUIDConverter::class)
abstract class DataBase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSyncBindDao(): YouTubePlaylistSynchronizationDao
}