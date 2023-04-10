package com.youtubemusic.core.model

import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Data class representing media item containing all necessary information
 */
@Parcelize
data class MediaItem(
    override val id: String,
    override val title: String,
    override val author: String,
    override val durationInMillis: Long,
    val description: String,
    val thumbnail: File,
    val mediaFile: File,
) : Item(id, title, author, durationInMillis)