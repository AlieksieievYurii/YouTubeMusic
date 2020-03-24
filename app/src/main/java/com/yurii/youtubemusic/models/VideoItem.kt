package com.yurii.youtubemusic.models

import java.io.Serializable

data class VideoItem(val videoId: String? = null,
                     val title: String? = null,
                     val authorChannelTitle: String? = null,
                     val thumbnail: String? = null) : Serializable