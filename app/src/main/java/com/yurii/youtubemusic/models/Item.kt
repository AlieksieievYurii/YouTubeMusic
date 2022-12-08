package com.yurii.youtubemusic.models

abstract class Item(open val id: String, open val title: String, open val author: String, open val durationInMillis: Long)