package com.youtubemusic.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemPlaylist(val id: Long, val name: String) : Parcelable {
    companion object {
        val ALL = MediaItemPlaylist(-1, "All")
    }
}
fun MediaItemPlaylist.isDefault() = this == MediaItemPlaylist.ALL