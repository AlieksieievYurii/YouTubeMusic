package com.youtubemusic.core.model

import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import java.util.Date

/**
 * Data class representing video item containing all necessary information
 */
@Parcelize
data class VideoItem(
    override val id: String,
    override val title: String,
    override val author: String,
    override val durationInMillis: Long,
    val description: String,
    val viewCount: BigInteger,
    val likeCount: BigInteger,
    val thumbnail: String,
    val normalThumbnail: String,
    val publishDate: Date
) : Item(id, title, author, durationInMillis)