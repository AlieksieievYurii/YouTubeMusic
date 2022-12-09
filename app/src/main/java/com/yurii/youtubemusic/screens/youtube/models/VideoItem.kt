package com.yurii.youtubemusic.screens.youtube.models

import com.google.api.services.youtube.model.Video
import com.yurii.youtubemusic.models.Item
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.Duration
import java.io.Serializable
import java.math.BigInteger

@Parcelize
data class VideoItem(
    val videoId: String,
    val videoTitle: String,
    val authorChannelTitle: String,
    val description: String,
    val videoDurationInMillis: Long,
    val viewCount: BigInteger,
    val likeCount: BigInteger,
    val thumbnail: String,
    val normalThumbnail: String
) : Item(videoId, videoTitle, authorChannelTitle, videoDurationInMillis) {

    companion object {
        fun createFrom(video: Video): VideoItem =
            VideoItem(
                videoId = video.id,
                videoTitle = video.snippet.title,
                description = video.snippet.description,
                videoDurationInMillis = Duration.parse(video.contentDetails.duration).toMillis(),
                viewCount = video.statistics.viewCount,
                likeCount = video.statistics.likeCount,
                authorChannelTitle = video.snippet.channelTitle,
                thumbnail = video.snippet.thumbnails.default.url,
                normalThumbnail = video.snippet.thumbnails.medium.url
            )
    }
}