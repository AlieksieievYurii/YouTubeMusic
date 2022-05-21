package com.yurii.youtubemusic.screens.youtube.models

import androidx.annotation.IntRange
import java.io.Serializable

data class Progress(@IntRange(from = 0, to = 100) var progress: Int, var currentSize: Long, var totalSize: Long) : Serializable {
    fun update(progress: Int, currentSize: Long, totalSize: Long) {
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