package com.yurii.youtubemusic.screens.youtube.models

import com.google.api.services.youtube.model.Video
import com.yurii.youtubemusic.models.Item
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.Duration
import java.math.BigInteger

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
) : Item(id, title, author, durationInMillis) {

    companion object {
        fun createFrom(video: Video): VideoItem =
            VideoItem(
                id = video.id,
                title = video.snippet.title,
                author = video.snippet.channelTitle,
                durationInMillis = Duration.parse(video.contentDetails.duration).toMillis(),
                description = video.snippet.description,
                viewCount = video.statistics.viewCount,
                likeCount = video.statistics.likeCount,
                thumbnail = video.snippet.thumbnails.default.url,
                normalThumbnail = video.snippet.thumbnails.medium.url
            )
    }
}