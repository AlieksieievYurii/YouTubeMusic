package com.yurii.youtubemusic.utilities

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R


@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) {
    //TODO Do researching to find a solution to execute in separated thread
    Picasso.get()
        .load(url)
        .error(R.drawable.ic_loadint_image_error)
        .into(view)
}