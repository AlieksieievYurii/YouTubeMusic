package com.yurii.youtubemusic.models

import com.google.api.services.youtube.model.Video
import java.io.Serializable
import java.math.BigInteger

data class VideoItem(
    val videoId: String,
    val title: String,
    val authorChannelTitle: String,
    val description: String,
    val duration: String,
    val viewCount: BigInteger,
    val likeCount: BigInteger,
    val thumbnail: String,
    val normalThumbnail: String
) : Serializable {

    companion object {
        fun createFrom(video: Video): VideoItem =
            VideoItem(
                videoId = video.id,
                title = video.snippet.title,
                description = video.snippet.description,
                duration = video.contentDetails.duration,
                viewCount = video.statistics.viewCount,
                likeCount = video.statistics.likeCount,
                authorChannelTitle = video.snippet.channelTitle,
                thumbnail = video.snippet.thumbnails.default.url,
                normalThumbnail = video.snippet.thumbnails.medium.url
            )

        fun createMock(): VideoItem =
            VideoItem("", "", "", "", "", BigInteger.ZERO, BigInteger.ZERO, "", "")
    }
}