package com.yurii.youtubemusic.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.File
import java.util.UUID

@Database(
    entities = [MediaItemEntity::class,
        PlaylistEntity::class,
        MediaItemPlayListAssignment::class,
        YouTubePlaylistSyncEntity::class,
        YouTubePlaylistSyncCrossRefToMediaPlaylist::class], version = 2
)
@TypeConverters(FileConverter::class, UUIDConverter::class)
abstract class DataBase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistSyncBindDao(): YouTubePlaylistSynchronizationDao
}

class FileConverter {
    @TypeConverter
    fun fileToPath(file: File): String = file.absolutePath

    @TypeConverter
    fun pathToFile(path: String) = File(path)
}

class UUIDConverter {
    @TypeConverter
    fun UUIDtoString(uuid: UUID): String = uuid.toString()

    @TypeConverter
    fun idStringToUUID(uuid: String?): UUID? = if (uuid != null) UUID.fromString(uuid) else null
}