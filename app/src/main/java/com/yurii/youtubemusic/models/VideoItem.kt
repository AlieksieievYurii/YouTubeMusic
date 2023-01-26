package com.yurii.youtubemusic.models

import com.google.api.services.youtube.model.Video
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.Duration
import java.math.BigInteger

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
    val normalThumbnail: String
) : Item(id, title, author, durationInMillis)

fun Video.toVideoItem() = VideoItem(
    id = id,
    title = snippet.title,
    author = snippet.channelTitle,
    durationInMillis = Duration.parse(contentDetails.duration).toMillis(),
    description = snippet.description,
    viewCount = statistics.viewCount,
    likeCount = statistics.likeCount,
    thumbnail = snippet.thumbnails.default.url,
    normalThumbnail = snippet.thumbnails.medium.url
)