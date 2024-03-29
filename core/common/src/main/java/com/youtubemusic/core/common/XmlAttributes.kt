package com.youtubemusic.core.common

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.RoundedCornersTransformation
import java.io.File


@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    view.load(url) {
        crossfade(true)
        transformations(RoundedCornersTransformation(10f, 10f, 10f, 10f))
        error(R.drawable.ic_loading_image_error)
    }
}

@BindingAdapter("imageSrc")
fun decodeImage(view: ImageView, file: File?) {
    file?.let {
        view.load(file) {
            crossfade(true)
            transformations(RoundedCornersTransformation(20f, 20f, 20f, 20f))
            error(R.drawable.ic_loading_image_error)
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

@BindingAdapter("textResId")
fun textResId(textView: TextView, stringResId: Int?) {
    if (stringResId != null)
        textView.setText(stringResId)
}

@BindingAdapter("startImageResId")
fun startImageResId(textView: TextView, imageResId: Int?) {
    if (imageResId != null)
        textView.setCompoundDrawablesWithIntrinsicBounds(
            ResourcesCompat.getDrawable(textView.resources, imageResId, null),
            null,
            null,
            null
        )
}