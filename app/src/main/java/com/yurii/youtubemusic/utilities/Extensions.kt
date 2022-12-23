package com.yurii.youtubemusic.utilities

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import java.io.File
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

/**
 * Creates parent folders of the file
 */
fun File.parentMkdir() {
    if (!parentFile!!.exists())
        parentFile!!.mkdirs()
}

/**
 * Iterates recursively over all file of the directory
 */
fun File.walkFiles() = walk().filter { it.isFile }

/**
 * Returns index of the item where the [filter] matches. Otherwise null
 */
fun <T> List<T>.findIndex(filter: (T) -> Boolean): Int? {
    forEachIndexed { index, item ->
        if (filter.invoke(item))
            return index
    }
    return null
}

fun Fragment.requireApplication(): Application {
    return requireActivity().application
}

fun <T> MutableList<T>.move(from: Int, to:Int) {
    this.add(to, this.removeAt(from))
}