package com.yurii.youtubemusic.models

import com.google.api.services.youtube.model.Video
import java.io.Serializable
import java.math.BigInteger

data class VideoItem(
    val videoId: String? = null,
    val title: String? = null,
    val authorChannelTitle: String? = null,
    val description: String? = null,
    val duration: String? = null,
    val viewCount: BigInteger? = null,
    val likeCount: BigInteger? = null,
    val disLikeCount: BigInteger? = null,
    val thumbnail: String? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoItem

        if (videoId != other.videoId) return false

        return true
    }

    override fun hashCode(): Int {
        return videoId?.hashCode() ?: 0
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
                thumbnail = video.snippet.thumbnails.default.url
            )
    }
}