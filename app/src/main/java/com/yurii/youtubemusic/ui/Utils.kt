package com.yurii.youtubemusic.ui

import android.util.TypedValue
import android.view.View

fun View.toPx(dp: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.context.resources.displayMetrics).toInt()