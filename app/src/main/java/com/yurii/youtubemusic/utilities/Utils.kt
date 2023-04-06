package com.yurii.youtubemusic.utilities

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.yurii.youtubemusic.R

fun createFromPathOrReturnMock(context: Context, path: String?): Drawable {
    return Drawable.createFromPath(path) ?: ContextCompat.getDrawable(context, R.drawable.ic_thumbnail_mock)!!
}


