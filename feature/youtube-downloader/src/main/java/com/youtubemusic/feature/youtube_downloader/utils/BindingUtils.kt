package com.youtubemusic.feature.youtube_downloader.utils

import com.youtubemusic.core.model.YouTubePlaylistPrivacyStatus
import com.youtubemusic.feature.youtube_downloader.R

fun getStringRepresentation(playlistPrivacyStatus: YouTubePlaylistPrivacyStatus?): Int? = when (playlistPrivacyStatus) {
    YouTubePlaylistPrivacyStatus.PUBLIC -> R.string.label_publick
    YouTubePlaylistPrivacyStatus.UNLISTED -> R.string.label_unlisted
    YouTubePlaylistPrivacyStatus.PRIVATE -> R.string.label_private
    else -> null
}

fun getCorespondentIcon(playlistPrivacyStatus: YouTubePlaylistPrivacyStatus?): Int? = when(playlistPrivacyStatus) {
    YouTubePlaylistPrivacyStatus.PUBLIC -> R.drawable.ic_public_12
    YouTubePlaylistPrivacyStatus.UNLISTED -> R.drawable.ic_link_12
    YouTubePlaylistPrivacyStatus.PRIVATE -> R.drawable.ic_lock_12
    else -> null
}