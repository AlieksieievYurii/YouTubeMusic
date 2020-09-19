package com.yurii.youtubemusic.mediaservice

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.yurii.youtubemusic.mediaservicetest.ALL_MUSIC_ITEMS
import java.lang.Exception

private const val TAG = "MediaBackgroundService"

const val CATEGORIES_CONTENT = "__youtube_musics_categories__"
const val EMPTY_CONTENT = "__empty__"

private const val VOLUME_DUCK = 0.2f
private const val VOLUME_NORMAL = 1.0f

private enum class AudioFocus {
    NoFocus,
    CanDuck,
    Focused
}

class MediaService : MediaBrowserServiceCompat() {
    private lateinit var musicProvider: MusicsProvider
    private lateinit var queueProvider: QueueProvider
    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null

    private var currentState = PlaybackStateCompat.STATE_NONE
    private var canPlayOnFocusGain = false
    private var currentAudioFocus = AudioFocus.NoFocus

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        musicProvider = MusicsProvider(baseContext)
        queueProvider = QueueProvider(mediaSession, musicProvider)
        updateCurrentPlaybackState()
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(applicationContext, MediaButtonReceiver::class.java)
        }

        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)

        mediaSession = MediaSessionCompat(baseContext, TAG, mediaButtonReceiver, null).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(MediaSessionCallBacks())
            setMediaButtonReceiver(pendingIntent)
        }
        sessionToken = mediaSession.sessionToken
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            EMPTY_CONTENT -> result.detach()
            CATEGORIES_CONTENT -> requestCategories(result)
            else -> requestMusicItemsByCategory(parentId, result)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(CATEGORIES_CONTENT, null)
        } else
            BrowserRoot(EMPTY_CONTENT, null)
    }

    private fun requestCategories(result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(musicProvider.getMusicCategories())
    }

    private fun requestMusicItemsByCategory(patentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (musicProvider.isMusicsInitialized)
            result.sendResult(musicProvider.getMusicsByCategory(patentId))
        else {
            result.detach()
            musicProvider.retrieveMusics(object : MusicsProvider.CallBack {
                override fun onLoadSuccessfully() = result.sendResult(musicProvider.getMusicsByCategory(patentId))
                override fun onFailedToLoad(error: Exception) = result.sendError(Bundle())
            })
        }
    }

    private fun updateCurrentPlaybackState() {
        val currentPlaybackState = getCurrentPlaybackStateBuilder().build()
        mediaSession.setPlaybackState(currentPlaybackState)
    }

    private fun setErrorState(@PlaybackStateCompat.ErrorCode errorCode: Int, errorMessage: String) {
        val errorPlaybackState = getCurrentPlaybackStateBuilder().apply {
            setErrorMessage(errorCode, errorMessage)
        }.build()

        mediaSession.setPlaybackState(errorPlaybackState)
    }

    private fun getCurrentPlaybackStateBuilder(): PlaybackStateCompat.Builder {
        val position: Long = mediaPlayer?.run { currentPosition.toLong() } ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN

        return PlaybackStateCompat.Builder().apply {
            setActions(getAvailableActions())
            setState(currentState, position, 1.0f, SystemClock.elapsedRealtime())
        }
    }

    private fun getAvailableActions(): Long {
        if (!musicProvider.isMusicsInitialized || musicProvider.isEmptyMusicsList())
            return PlaybackStateCompat.ACTION_PREPARE

        var actions: Long = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        if (currentState == PlaybackStateCompat.STATE_PLAYING)
            actions = actions or PlaybackStateCompat.ACTION_PAUSE

        if (queueProvider.canMoveToPrevious())
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        if (queueProvider.canMoveToNext())
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT

        return actions
    }

    private inner class MediaSessionCallBacks : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.i(TAG, "OnPlay")
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            Log.i(TAG, "onPlayFromMediaId")
        }

        override fun onStop() {
            super.onStop()
            Log.i(TAG, "OnStop")
        }

        override fun onPause() {
            super.onPause()
            Log.i(TAG, "OnPause")
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.i(TAG, "Next")
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            Log.i(TAG, "Previous")
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            Log.i(TAG, "OnSeek to $pos")
        }
    }

    private inner class AudioFocusChanges : AudioManager.OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                }
            }
        }
    }

    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && mediaPlayer?.isPlaying == true)
                mediaPlayer?.pause()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }
}