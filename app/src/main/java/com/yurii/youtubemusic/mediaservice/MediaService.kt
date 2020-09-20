package com.yurii.youtubemusic.mediaservice

import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.yurii.youtubemusic.mediaservice.MusicsProvider.Companion.METADATA_TRACK_SOURCE
import java.lang.Exception
import java.lang.IllegalStateException

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
    private lateinit var audioManager: AudioManager
    private lateinit var musicProvider: MusicsProvider
    private lateinit var queueProvider: QueueProvider
    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null
    private var currentState = PlaybackStateCompat.STATE_NONE
    private var canPlayOnFocusGain = false
    private var currentAudioFocus = AudioFocus.NoFocus
    private val becomingNoisyReceiver = BecomingNoisyReceiver()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        val categories = musicProvider.getMusicCategories()
        result.sendResult(categories)
    }

    private fun requestMusicItemsByCategory(patentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (musicProvider.isMusicsInitialized)
            result.sendResult(musicProvider.getMusicsByCategory(patentId))
        else {
            result.detach()
            musicProvider.retrieveMusics(object : MusicsProvider.CallBack {
                override fun onLoadSuccessfully() = result.sendResult(musicProvider.getMusicsByCategory(patentId))
                override fun onFailedToLoad(error: Exception) = setErrorState(PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED, error.message!!)
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

    private fun getMediaPlayer(): MediaPlayer =
        mediaPlayer ?: throw IllegalStateException("Cannot return mediaPlayer because it's null in state code: $currentState")


    private fun resetOrCreateMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val listeners = MediaPlayerCallBacks()
                setOnPreparedListener(listeners)
                setOnCompletionListener(listeners)
                setOnErrorListener(listeners)
            }
        } else
            getMediaPlayer().reset()
    }


    @Suppress("DEPRECATION")
    private fun giveUpAudioFocus() {
        if (currentAudioFocus == AudioFocus.Focused && audioManager.abandonAudioFocus(audioFocus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocus = AudioFocus.NoFocus
        }
    }


    @Suppress("DEPRECATION")
    private fun tryToGetAudioFocus() {
        Log.i(TAG, "tryToGetAudioFocus")
        if (currentAudioFocus == AudioFocus.NoFocus) {
            val result = audioManager.requestAudioFocus(audioFocus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            currentAudioFocus = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AudioFocus.Focused else AudioFocus.NoFocus
        }
    }

    private fun configMediaPlayerState() {
        when (currentAudioFocus) {
            AudioFocus.NoFocus -> pauseMediaPlayer()
            AudioFocus.CanDuck -> getMediaPlayer().setVolume(VOLUME_DUCK, VOLUME_DUCK)
            AudioFocus.Focused -> getMediaPlayer().apply {
                setVolume(VOLUME_NORMAL, VOLUME_NORMAL)
                if (canPlayOnFocusGain)
                    playMediaPlayer()
            }
        }
    }

    private fun handlePlayMusicQueue() {
        Log.i(TAG, "Call: handlePlayMusicQueue")

        tryToGetAudioFocus()
        activeMediaSession()
        prepareMusicFromQueue()
    }

    private fun prepareMusicFromQueue() {
        currentState = PlaybackStateCompat.STATE_BUFFERING
        val metadata = queueProvider.getCurrentQueueItemMetaData()
        updateCurrentPlaybackState()
        mediaSession.setMetadata(metadata)
        resetOrCreateMediaPlayer()

        getMediaPlayer().apply {
            setDataSource(metadata.getString(METADATA_TRACK_SOURCE))
            prepareAsync()
        }
    }

    private fun playMediaPlayer() {
        currentState = PlaybackStateCompat.STATE_PLAYING
        canPlayOnFocusGain = false
        getMediaPlayer().start()
        updateCurrentPlaybackState()
    }

    private fun pauseMediaPlayer() {
        currentState = PlaybackStateCompat.STATE_PAUSED
        getMediaPlayer().pause()
        updateCurrentPlaybackState()
    }

    private fun handleStopRequest() {
        currentState = PlaybackStateCompat.STATE_STOPPED

        getMediaPlayer().apply {
            reset()
            release()
            mediaPlayer = null
        }

        giveUpAudioFocus()
        updateCurrentPlaybackState()
        stopSelf()
    }

    private fun activeMediaSession() {
        if (!mediaSession.isActive)
            mediaSession.isActive = true
    }

    private inner class MediaSessionCallBacks : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.i(TAG, "OnPlay")
            registerReceiver(becomingNoisyReceiver, becomingNoisyReceiver.becomingNoisyIntent)
            playMediaPlayer()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            val category = extras?.getString("KEY_CATEGORY") ?: "all"
            Log.i(TAG, "onPlayFromMediaId mediaId=$mediaId category=$category")

            queueProvider.createQueue(mediaId, category)
            registerReceiver(becomingNoisyReceiver, becomingNoisyReceiver.becomingNoisyIntent)
            handlePlayMusicQueue()
        }


        override fun onStop() {
            super.onStop()
            Log.i(TAG, "OnStop")
            handleStopRequest()
            unregisterReceiver(becomingNoisyReceiver)
        }

        override fun onPause() {
            super.onPause()
            Log.i(TAG, "onPause")
            pauseMediaPlayer()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.i(TAG, "Next")

            if (queueProvider.canMoveToNext())
                queueProvider.moveToNextQueueItem()
            else
                queueProvider.setOnFirstPosition()

            handlePlayMusicQueue()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            Log.i(TAG, "Previous")

            if (queueProvider.canMoveToPrevious())
                queueProvider.moveToPreviousQueueItem()
            else
                queueProvider.setOnLastPosition()

            handlePlayMusicQueue()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            Log.i(TAG, "OnSeek to $pos")
        }
    }

    private inner class MediaPlayerCallBacks : MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
        override fun onPrepared(mp: MediaPlayer?) {
            Log.i(TAG, "Music has been prepared!")
            playMediaPlayer()
        }

        override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
            TODO("Not yet implemented")
        }

        override fun onCompletion(mp: MediaPlayer?) {
            if (queueProvider.isQueueEmpty()) {
                handleStopRequest()
            } else if (!queueProvider.canMoveToNext()) {
                queueProvider.setOnFirstPosition()
                handlePlayMusicQueue()
            } else {
                queueProvider.moveToNextQueueItem()
                handlePlayMusicQueue()
            }
        }
    }

    private val audioFocus = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> currentAudioFocus = AudioFocus.Focused

            AudioManager.AUDIOFOCUS_LOSS -> currentAudioFocus = AudioFocus.NoFocus

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                currentAudioFocus = AudioFocus.NoFocus
                canPlayOnFocusGain = true
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> currentAudioFocus = AudioFocus.CanDuck
        }
        configMediaPlayerState()
    }

    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        val becomingNoisyIntent = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && mediaPlayer?.isPlaying == true)
                pauseMediaPlayer()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }
}