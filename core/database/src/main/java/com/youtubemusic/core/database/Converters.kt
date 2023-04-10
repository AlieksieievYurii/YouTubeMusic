package com.youtubemusic.core.database

import androidx.room.TypeConverter
import java.io.File
import java.util.*

internal class FileConverter {
    @TypeConverter
    fun fileToPath(file: File): String = file.absolutePath

    @TypeConverter
    fun pathToFile(path: String) = File(path)
}

internal class UUIDConverter {
    @TypeConverter
    fun uUIDtoString(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun idStringToUUID(uuid: String?): UUID? = if (uuid != null) UUID.fromString(uuid) else null
}