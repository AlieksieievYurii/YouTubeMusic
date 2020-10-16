package com.yurii.youtubemusic.mediaservice

import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.os.ResultReceiver
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.yurii.youtubemusic.MainActivity
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.EXTRA_KEY_CATEGORIES
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.models.toMediaMetadataCompat

private const val TAG = "MediaBackgroundService"

const val CATEGORIES_CONTENT = "__youtube_musics_categories__"
const val EMPTY_CONTENT = "__empty__"

const val REQUEST_COMMAND_ADD_NEW_MEDIA_ITEM = "__request_command_add_new_media_item"
const val REQUEST_COMMAND_DELETE_MEDIA_ITEM = "__request_command_delete_media_item"
const val REQUEST_COMMAND_UPDATE_MEDIA_ITEMS = "__request_command_update_media_items"
const val REQUEST_CODE_UPDATE_MEDIA_ITEMS = 1001

const val PLAYBACK_STATE_MEDIA_ITEM = "com.yurii.youtubemusic.playback.state.media.item"
const val EXTRA_MEDIA_ITEM = "com.yurii.youtubemusic.playback.media.item"

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
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null
    private var currentState = PlaybackStateCompat.STATE_NONE
    private var canPlayOnFocusGain = false
    private var currentAudioFocus = AudioFocus.NoFocus
    private val becomingNoisyReceiver = BecomingNoisyReceiver()
    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initMediaSession()
        musicProvider = MusicsProvider(baseContext)
        queueProvider = QueueProvider(mediaSession, musicProvider)
        notificationManager = NotificationManager(baseContext, sessionToken!!)
        updateCurrentPlaybackState()
    }

    private fun initMediaSession() {
        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            sessionIntent.putExtra(MainActivity.EXTRA_LAUNCH_FRAGMENT, MainActivity.EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC)
            PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(applicationContext, MediaButtonReceiver::class.java)
        }

        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)

        mediaSession = MediaSessionCompat(baseContext, TAG, mediaButtonReceiver, null).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(MediaSessionCallBacks())
            setMediaButtonReceiver(pendingIntent)
            setSessionActivity(sessionActivityPendingIntent)
        }
        sessionToken = mediaSession.sessionToken
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
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

    private fun requestCategories(result: Result<List<MediaBrowserCompat.MediaItem>>) {
        val categories = musicProvider.getMediaItemsCategories()
        result.sendResult(categories)
    }

    private fun requestMusicItemsByCategory(patentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        if (musicProvider.isMusicsInitialized)
            result.sendResult(musicProvider.getMediaCompactItemsByCategoryId(patentId.toInt()))
        else {
            result.detach()
            musicProvider.retrieveMusics(object : MusicsProvider.CallBack {
                override fun onLoadSuccessfully() = result.sendResult(musicProvider.getMediaCompactItemsByCategoryId(patentId.toInt()))
                override fun onFailedToLoad(error: Exception) = setErrorState(PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED, error.message!!)
            })
        }
    }

    private fun updateCurrentPlaybackState() {
        val extras = Bundle().apply {
            if (currentState == PlaybackStateCompat.STATE_PLAYING || currentState == PlaybackStateCompat.STATE_PAUSED)
                putParcelable(PLAYBACK_STATE_MEDIA_ITEM, queueProvider.getQueue().getCurrentQueueItem())
        }
        val currentPlaybackState = getCurrentPlaybackStateBuilder().apply {
            setExtras(extras)
        }.build()
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

        return playbackStateBuilder.apply {
            setActions(getAvailableActions())
            setState(currentState, position, 1.0f, SystemClock.elapsedRealtime())
        }
    }

    private fun getAvailableActions(): Long {
        if (!musicProvider.isMusicsInitialized || musicProvider.isEmptyMusicsList())
            return PlaybackStateCompat.ACTION_PREPARE

        var actions: Long = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        if (!queueProvider.queueExists())
            return actions

        if (currentState == PlaybackStateCompat.STATE_PLAYING)
            actions = actions or PlaybackStateCompat.ACTION_PAUSE

        if (queueProvider.getQueue().canMoveToPrevious())
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        if (queueProvider.getQueue().canMoveToNext())
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
        tryToGetAudioFocus()
        mediaSession.isActive = true
        prepareMusicFromQueue()
    }

    private fun prepareMusicFromQueue() {
        currentState = PlaybackStateCompat.STATE_BUFFERING
        val metadata: MediaMetaData = queueProvider.getQueue().getCurrentQueueItem()
        updateCurrentPlaybackState()
        mediaSession.setMetadata(metadata.toMediaMetadataCompat())
        resetOrCreateMediaPlayer()

        getMediaPlayer().apply {
            setDataSource(metadata.mediaFile.absolutePath)
            prepareAsync()
        }
    }

    private fun playMediaPlayer() {
        currentState = PlaybackStateCompat.STATE_PLAYING
        canPlayOnFocusGain = false
        getMediaPlayer().start()
        notificationManager.showPlayingNotification()
        startForeground(NotificationManager.NOTIFICATION_ID, notificationManager.getCurrentNotification())
        updateCurrentPlaybackState()
    }

    private fun pauseMediaPlayer() {
        currentState = PlaybackStateCompat.STATE_PAUSED
        getMediaPlayer().pause()
        notificationManager.showPauseNotification()
        stopForeground(false)
        updateCurrentPlaybackState()
    }

    private fun handleStopRequest() {
        currentState = PlaybackStateCompat.STATE_STOPPED

        getMediaPlayer().apply {
            reset()
            release()
            mediaPlayer = null
        }

        stopForeground(true)
        giveUpAudioFocus()
        updateCurrentPlaybackState()
        stopSelf()
    }

    private fun deleteMediaItem(mediaId: String) {
        musicProvider.deleteMediaItem(mediaId)
        if (queueProvider.queueExists()) {
            if (queueProvider.getQueue().getCurrentQueueItem().mediaId == mediaId)
                handleStopRequest()
            queueProvider.getQueue().deleteMediaItemIfExistsInQueue(mediaId)
        }
    }

    private fun addNewItemToQueue(mediaId: String) {
        if (musicProvider.isMusicsInitialized)
            musicProvider.addNewMediaItem(mediaId)

        if (queueProvider.queueExists())
            queueProvider.addMediaItemToQueue(mediaId)

        updateCurrentPlaybackState()
    }

    private inner class MediaSessionCallBacks : MediaSessionCompat.Callback() {
        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)
            when (command) {
                REQUEST_COMMAND_ADD_NEW_MEDIA_ITEM -> {
                    val mediaId = extras?.getString(EXTRA_MEDIA_ITEM) ?: throw IllegalStateException("MediaId is required")
                    addNewItemToQueue(mediaId)
                }
                REQUEST_COMMAND_DELETE_MEDIA_ITEM -> {
                    val mediaId = extras?.getString(EXTRA_MEDIA_ITEM) ?: throw IllegalStateException("MediaId is required")
                    deleteMediaItem(mediaId)
                }
                REQUEST_COMMAND_UPDATE_MEDIA_ITEMS -> {
                    musicProvider.updateMediaItems()
                    cb?.send(REQUEST_CODE_UPDATE_MEDIA_ITEMS, null)
                }
            }
        }

        override fun onPlay() {
            super.onPlay()
            registerReceiver(becomingNoisyReceiver, becomingNoisyReceiver.becomingNoisyIntent)
            if (currentState == PlaybackStateCompat.STATE_PAUSED) {
                tryToGetAudioFocus()
                playMediaPlayer()
            }
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            val category: Category = extras?.getParcelable(EXTRA_KEY_CATEGORIES) ?: Category.ALL

            queueProvider.createQueue(mediaId, category)
            registerReceiver(becomingNoisyReceiver, becomingNoisyReceiver.becomingNoisyIntent)
            handlePlayMusicQueue()
        }


        override fun onStop() {
            super.onStop()
            handleStopRequest()
            unregisterReceiver(becomingNoisyReceiver)
        }

        override fun onPause() {
            super.onPause()
            giveUpAudioFocus()
            pauseMediaPlayer()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            val queue = queueProvider.getQueue()

            if (queue.canMoveToNext())
                queue.next()
            else
                queue.setFirstPosition()

            handlePlayMusicQueue()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            val queue = queueProvider.getQueue()

            if (queue.canMoveToPrevious())
                queue.previous()
            else
                queue.setLastPosition()

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
            Toast.makeText(applicationContext, "Error has been occured", Toast.LENGTH_LONG).show()
            return true
        }

        override fun onCompletion(mp: MediaPlayer?) {
            val queue = queueProvider.getQueue()
            when {
                queue.isQueueEmpty() -> handleStopRequest()

                queue.canMoveToNext() -> {
                    queue.next()
                    handlePlayMusicQueue()
                }
                else -> {
                    queue.setFirstPosition()
                    handlePlayMusicQueue()
                }
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

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.run {
            isActive = false
            release()
        }
    }

    private inner class BecomingNoisyReceiver : BroadcastReceiver() {
        val becomingNoisyIntent = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && mediaPlayer?.isPlaying == true) {
                pauseMediaPlayer()
                giveUpAudioFocus()
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }
}