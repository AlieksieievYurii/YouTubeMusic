package com.yurii.youtubemusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Application : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory


    override fun onCreate() {
        super.onCreate()
        createMediaPlayerNotificationChannel()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setWorkerFactory(workerFactory).build()
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

    companion object {
        const val MEDIA_PLAYER_NOTIFICATION_CHANNEL = "com.yurii.youtubemusic.media.player.notification"
    }
}