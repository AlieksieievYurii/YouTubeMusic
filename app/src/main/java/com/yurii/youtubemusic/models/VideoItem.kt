package com.yurii.youtubemusic.models

import com.google.api.services.youtube.model.Video
import java.io.Serializable
import java.lang.Exception
import java.math.BigInteger

data class VideoItem(
    val videoId: String,
    val title: String,
    val authorChannelTitle: String,
    val description: String,
    val duration: String,
    val viewCount: BigInteger,
    val likeCount: BigInteger,
    val disLikeCount: BigInteger,
    val thumbnail: String,
    val normalThumbnail: String
) : Serializable {
    var lastError: Exception? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoItem

        if (videoId != other.videoId) return false

        return true
    }

    override fun hashCode(): Int {
        return videoId.hashCode()
    }

    companion object {
        fun createFrom(video: Video): VideoItem =
            VideoItem(
                videoId = video.id,
                title = video.snippet.title,
                description = video.snippet.description,
                duration = video.contentDetails.duration,
                viewCount = video.statistics.viewCount,
                likeCount = video.statistics.likeCount,
                disLikeCount = video.statistics.dislikeCount,
                authorChannelTitle = video.snippet.channelTitle,
                thumbnail = video.snippet.thumbnails.default.url,
                normalThumbnail = video.snippet.thumbnails.medium.url
            )

        fun createMock(): VideoItem =
            VideoItem("", "", "", "", "", BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, "", "")
    }
}