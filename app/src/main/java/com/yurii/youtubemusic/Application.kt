package com.yurii.youtubemusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        createMediaPlayerNotificationChannel()
        createMusicsDownloadingNotificationChannel()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun createMediaPlayerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MediaPlayerNotification"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(MEDIA_PLAYER_NOTIFICATION_CHANNEL, name, importance).apply {
                description = "Notification description"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createMusicsDownloadingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "YoutubeMusicDownloaderNotification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(YOUTUBE_DOWNLOADER_NOTIFICATION_CHANNEL, name, importance).apply {
                description = "Notification description"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val MEDIA_PLAYER_NOTIFICATION_CHANNEL = "com.yurii.youtubemusic.media.player.notification"
        const val YOUTUBE_DOWNLOADER_NOTIFICATION_CHANNEL = "com.yurii.youtubemusic.youtube.downloader.notification"
    }
}