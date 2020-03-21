package com.yurii.youtubemusic.models

import java.io.Serializable

data class VideoItem(val videoId: String,
                     val title: String,
                     val authorChannelTitle: String,
                     val thumbnail: String) : Serializable