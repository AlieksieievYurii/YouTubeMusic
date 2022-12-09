package com.yurii.youtubemusic.utilities

import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalStateException

fun <T : RecyclerView.ViewHolder> RecyclerView.getVisibleItems(): List<T> {
    val arrayList = ArrayList<T>()
    for (index: Int in 0 until childCount) {
        val child = getChildAt(index)
        val position = getChildAdapterPosition(child)

        if (position == RecyclerView.NO_POSITION)
            continue

        val viewHolder = getChildViewHolder(child) as T
        arrayList.add(viewHolder)
    }

    return arrayList
}

fun <T : Parcelable> Bundle.requireParcelable(key: String): T {
    return this.getParcelable(key) ?: throw IllegalStateException("This bundle argument is required: $key")
}