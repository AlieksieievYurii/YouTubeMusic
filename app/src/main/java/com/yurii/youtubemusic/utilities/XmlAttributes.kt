package com.yurii.youtubemusic.utilities

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.RoundedCornersTransformation
import com.yurii.youtubemusic.R
import java.io.File


@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) {
    view.load(url) {
        crossfade(true)
        error(R.drawable.ic_loadint_image_error)
    }
}

@BindingAdapter("imageSrc")
fun decodeImage(view: ImageView, file: File?) {
    file?.let {
        view.load(file) {
            crossfade(true)
            transformations(RoundedCornersTransformation(20f, 20f, 20f, 20f))
            error(R.drawable.ic_loadint_image_error)
        }
    }
}

@BindingAdapter("animatedText")
fun animatedText(textView: TextView, text: String?) {
    text?.let { textView.setAnimatedText(it) }
}

@BindingAdapter("isVisible")
fun isVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}