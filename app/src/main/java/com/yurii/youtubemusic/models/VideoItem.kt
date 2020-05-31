package com.yurii.youtubemusic.models

import java.io.Serializable
import java.math.BigInteger

data class VideoItem(val videoId: String? = null,
                     val title: String? = null,
                     val authorChannelTitle: String? = null,
                     val description: String? = null,
                     val duration: String? = null,
                     val viewCount: BigInteger? = null,
                     val likeCount: BigInteger? = null,
                     val disLikeCount: BigInteger? = null,
                     val thumbnail: String? = null) : Serializable {
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
}