package com.yurii.youtubemusic.services.downloader

import java.io.Serializable

/**
 * Class represents a progress of downloading item(Audio from video)
 * [progress] - Current progress from 0 to 100.
 * [currentSize] - Current loaded size in bytes. [currentSizeInMb] returns [currentSize] in megabytes
 * [totalSize] - Total size of downloading item in bytes. [totalSizeInMb] returns [totalSize] in megabytes
 */
data class Progress(var progress: Int, var currentSize: Int, var totalSize: Int) : Serializable {
    val currentSizeInMb: Double
        get() = currentSize.toDouble() / 1000_000
    val totalSizeInMb: Double
        get() = totalSize.toDouble() / 1000_000

    fun update(progress: Int, currentSize: Int, totalSize: Int) {
        this.progress = progress
        this.currentSize = currentSize
        this.totalSize = totalSize
    }

    companion object {
        fun create(): Progress {
            return Progress(0, 0, 0)
        }
    }
}