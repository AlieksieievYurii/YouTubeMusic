package com.yurii.youtubemusic.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(val name: String) : Parcelable {
    companion object {
        val ALL = Category("all")
    }
}