package com.yurii.youtubemusic.utilities

import android.content.res.ColorStateList
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
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

inline fun <reified T : Parcelable> Bundle.requireParcelable(key: String): T {
    return this.parcelable(key) ?: throw IllegalStateException("This bundle argument is required: $key")
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

fun <T> MutableList<T>.move(from: Int, to: Int) {
    this.add(to, this.removeAt(from))
}

/**
 * Sets given [text] to the textview with fade in/out animation
 */
fun TextView.setAnimatedText(text: String) {
    if (text == this.text)
        return

    val anim = AlphaAnimation(1.0f, 0.0f).apply {
        duration = 200
        repeatCount = 1
        repeatMode = Animation.REVERSE
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                // Nothing
            }

            override fun onAnimationEnd(p0: Animation?) {
                // Nothing
            }

            override fun onAnimationRepeat(p0: Animation?) {
                this@setAnimatedText.text = text
            }
        })
    }
    startAnimation(anim)
}

/**
 * Alternative to ImageView.tint in xml
 */
fun ImageView.setTint(@ColorRes colorRes: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : java.io.Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= 33 -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}