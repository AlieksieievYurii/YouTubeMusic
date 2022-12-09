package com.yurii.youtubemusic.models

import android.os.Parcelable

/**
 * An abstract class that is supposed to be extended by the item
 * representing some media item e.g video item and media(music) item
 */
abstract class Item(
    @Transient open val id: String,
    @Transient open val title: String,
    @Transient open val author: String,
    @Transient open val durationInMillis: Long
) : Parcelable