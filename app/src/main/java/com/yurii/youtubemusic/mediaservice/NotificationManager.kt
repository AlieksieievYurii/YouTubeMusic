package com.yurii.youtubemusic.mediaservice

import android.app.Notification
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import com.yurii.youtubemusic.Application
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.utilities.createFromPathOrReturnMock
import java.lang.IllegalStateException

class NotificationManager(private val context: Context, private val sessionToken: MediaSessionCompat.Token) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val mediaController = MediaControllerCompat(context, sessionToken)

    private val pauseAction = NotificationCompat.Action(
        android.R.drawable.ic_media_pause,
        context.getString(R.string.label_action_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    )

    private val playAction = NotificationCompat.Action(
        android.R.drawable.ic_media_play,
        context.getString(R.string.label_action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    )

    private val nextAction = NotificationCompat.Action(
        android.R.drawable.ic_media_next,
        context.getString(R.string.label_action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    private val previousAction = NotificationCompat.Action(
        android.R.drawable.ic_media_previous,
        context.getString(R.string.label_action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    private var currentNotification: Notification? = null

    fun getCurrentNotification(): Notification = currentNotification ?: throw IllegalStateException("Notification is null")

    fun showPlayingNotification() {
        currentNotification = getNotificationBuilder().apply {
            addAction(previousAction)
            addAction(pauseAction)
            addAction(nextAction)

        }.build()
        notificationManager.notify(NOTIFICATION_ID, currentNotification!!)
    }

    fun showPauseNotification() {
        currentNotification = getNotificationBuilder().apply {
            addAction(previousAction)
            addAction(playAction)
            addAction(nextAction)

        }.build()
        notificationManager.notify(NOTIFICATION_ID, currentNotification!!)
    }

    private fun getNotificationBuilder() = NotificationCompat.Builder(context, Application.MEDIA_PLAYER_NOTIFICATION_CHANNEL).apply {
        setSmallIcon(R.drawable.ic_first_selection_illustration)
        setShowWhen(false)
        setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(sessionToken)
        )
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setSmallIcon(R.mipmap.ic_launcher)
        setContentIntent(mediaController.sessionActivity)
        setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))

        setContentTitle(mediaController.metadata.description.title)
        setContentText(mediaController.metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR))
        setLargeIcon(createFromPathOrReturnMock(context, mediaController.metadata.description.iconUri!!.encodedPath!!).toBitmap())
    }


    companion object {
        const val NOTIFICATION_ID = 8888
    }
}