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
                     val thumbnail: String? = null,
                     var downloadingProgress: Int = 0) : Serializable