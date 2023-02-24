package com.yurii.youtubemusic.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.File

@Database(entities = [MediaItemEntity::class, PlaylistEntity::class, MediaItemPlayListAssignment::class], version = 1)
@TypeConverters(FileConverter::class)
abstract class DataBase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
}

class FileConverter {
    @TypeConverter
    fun fileToPath(file: File): String = file.absolutePath

    @TypeConverter
    fun pathToFile(path: String) = File(path)
}