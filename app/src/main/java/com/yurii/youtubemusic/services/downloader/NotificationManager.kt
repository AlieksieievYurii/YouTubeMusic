package com.yurii.youtubemusic.services.downloader

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yurii.youtubemusic.Application
import com.yurii.youtubemusic.R

class NotificationManager(context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationBuilder = NotificationCompat.Builder(context, Application.YOUTUBE_DOWNLOADER_NOTIFICATION_CHANNEL)
    private val currentNotification: Notification? = null
    fun getNotification(): Notification {
        if (currentNotification != null)
            return currentNotification
        notificationBuilder.apply {
            setContentTitle("Downloading musics")
            setContentText("Dupa")
            setSmallIcon(R.drawable.ic_play_24dp)
            setProgress(100, 50, false)
        }
        return notificationBuilder.build()
    }
}