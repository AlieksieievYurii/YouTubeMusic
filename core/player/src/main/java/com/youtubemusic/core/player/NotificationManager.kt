package com.youtubemusic.core.player

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import java.lang.IllegalStateException

class NotificationManager(private val context: Context, private val sessionToken: MediaSessionCompat.Token) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val mediaController = MediaControllerCompat(context, sessionToken)

    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_48px,
        context.getString(R.string.label_action_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    )

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_48dp,
        context.getString(R.string.label_action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_48dp,
        context.getString(R.string.label_action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    private val previousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_48dp,
        context.getString(R.string.label_action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    private var currentNotification: Notification? = null

    fun getCurrentNotification(): Notification = currentNotification ?: throw IllegalStateException("Notification is null")

    @SuppressLint("MissingPermission")
    fun showPlayingNotification() {
        currentNotification = getNotificationBuilder().apply {
            addAction(previousAction)
            addAction(pauseAction)
            addAction(nextAction)

        }.build()
        notificationManager.notify(NOTIFICATION_ID, currentNotification!!)
    }

    @SuppressLint("MissingPermission")
    fun showPauseNotification() {
        currentNotification = getNotificationBuilder().apply {
            addAction(previousAction)
            addAction(playAction)
            addAction(nextAction)

        }.build()
        notificationManager.notify(NOTIFICATION_ID, currentNotification!!)
    }

    private fun getNotificationBuilder() = NotificationCompat.Builder(context, MEDIA_PLAYER_NOTIFICATION_CHANNEL).apply {
        setSmallIcon(R.drawable.ic_thumbnail_mock)
        setShowWhen(false)
        setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(sessionToken)
        )
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setSmallIcon(R.drawable.ic_playing_music_24px)
        setContentIntent(mediaController.sessionActivity)
        setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))

        setContentTitle(mediaController.metadata.description.title)
        setContentText(mediaController.metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR))
        setLargeIcon(createFromPathOrReturnMock(context, mediaController.metadata.description.iconUri!!.encodedPath!!).toBitmap())
    }

    private fun createFromPathOrReturnMock(context: Context, path: String?): Drawable {
        return Drawable.createFromPath(path) ?: ContextCompat.getDrawable(context, R.drawable.ic_thumbnail_mock)!!
    }

    companion object {
        const val NOTIFICATION_ID = 8888
        const val MEDIA_PLAYER_NOTIFICATION_CHANNEL = "com.yurii.youtubemusic.media.player.notification"
    }
}