package com.yurii.youtubemusic.services.downloader

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.IntRange
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yurii.youtubemusic.Application
import com.yurii.youtubemusic.MainActivity
import com.yurii.youtubemusic.R

class NotificationManager(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationBuilder = NotificationCompat.Builder(context, Application.YOUTUBE_DOWNLOADER_NOTIFICATION_CHANNEL)
    private val sessionActivityPendingIntent = context.packageManager?.getLaunchIntentForPackage(context.packageName)?.let { sessionIntent ->
        sessionIntent.putExtra(MainActivity.EXTRA_LAUNCH_FRAGMENT, MainActivity.EXTRA_LAUNCH_FRAGMENT_YOUTUBE_MUSIC)
        PendingIntent.getActivity(context, 0, sessionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun buildNotification(@IntRange(from = 0, to = 100) progress: Int): Notification {
        notificationBuilder.apply {
            setContentTitle(context.getString(R.string.label_downloading_music))
            setContentText("$progress %")
            setSmallIcon(R.drawable.ic_downloading)
            setContentIntent(sessionActivityPendingIntent)
            setProgress(100, progress, false)
        }
        return notificationBuilder.build()
    }

    fun updateProgress(progress: Int) {
        val notification = buildNotification(progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 7777
    }
}