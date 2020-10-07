package com.yurii.youtubemusic.utilities

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R


@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) {
    Picasso.get()
        .load(url)
        .error(R.drawable.ic_loadint_image_error)
        .into(view)
}

@BindingAdapter("imageSrc")
fun decodeImage(view: ImageView, path: String) {
    view.setImageDrawable(createFromPathOrReturnMock(view.context, path))
}