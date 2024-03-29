package com.youtubemusic.core.common

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.lang.IllegalStateException
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
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
fun ImageView.setTint(colorRes: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(colorRes))
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : java.io.Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= 33 -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

fun Service.stopForegroundCompat(removeNotification: Boolean) {
    if (SDK_INT >= Build.VERSION_CODES.N)
        stopForeground(if (removeNotification) Service.STOP_FOREGROUND_REMOVE else Service.STOP_FOREGROUND_DETACH)
    else
        @Suppress("DEPRECATION") stopForeground(removeNotification)
}

fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = Observer<T> { value -> trySend(value) }
    observeForever(observer)
    awaitClose {
        removeObserver(observer)
    }
}.flowOn(Dispatchers.Main.immediate)

fun <T, R> Flow<List<T>>.mapItems(transform: (T) -> R): Flow<List<R>> {
    return map { it.map(transform) }
}

fun ImageView.setUniqueAnimatedDrawable(animatedVectorDrawableResId: Int) {
    if (tag != animatedVectorDrawableResId) {
        setImageResource(animatedVectorDrawableResId)
        (drawable as? AnimatedVectorDrawable)?.start()
            ?: throw IllegalStateException("Given drawable ID is not animated vector")
        tag = animatedVectorDrawableResId
    }
}

fun Context.getAttrColor(attr: Int): Int {
    val typedValue = TypedValue()
    val a: TypedArray = obtainStyledAttributes(typedValue.data, intArrayOf(attr))
    val color = a.getColor(0, 0)
    a.recycle()
    return color
}

@SuppressLint("UnsafeOptInUsageError")
fun Toolbar.attachNumberBadge(menuItemId: Int, lifecycleOwner: LifecycleOwner, target: Flow<Int>) {
    val downloadManagerBudge = BadgeDrawable.create(this@attachNumberBadge.context)
    lifecycleOwner.lifecycleScope.launchWhenStarted {
        target.collectLatest {
            if (it != 0) {
                downloadManagerBudge.isVisible = true
                downloadManagerBudge.number = it
            } else
                downloadManagerBudge.isVisible = false
        }
    }
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            BadgeUtils.attachBadgeDrawable(downloadManagerBudge, this@attachNumberBadge, menuItemId)
        }

        override fun onStop(owner: LifecycleOwner) {
            BadgeUtils.detachBadgeDrawable(downloadManagerBudge, this@attachNumberBadge, menuItemId)
        }
    })
}

@SuppressLint("DiscouragedPrivateApi")
fun SearchView.setWhiteCursor() {
    val searchTextView: SearchView.SearchAutoComplete = findViewById(androidx.appcompat.R.id.search_src_text)
    if (SDK_INT >= Build.VERSION_CODES.Q) {
        searchTextView.setTextCursorDrawable(R.drawable.cursor)
    } else {
        try {
            val field: Field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            field.isAccessible = true
            field.set(searchTextView, R.drawable.cursor)
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
        }
    }
}