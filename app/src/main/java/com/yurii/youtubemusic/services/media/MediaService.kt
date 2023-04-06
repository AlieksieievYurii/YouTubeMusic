package com.yurii.youtubemusic.services.media

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.youtubemusic.core.common.parcelable
import com.youtubemusic.core.common.stopForegroundCompat
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.screens.main.MainActivity
import com.yurii.youtubemusic.source.MediaLibraryDomain
import com.yurii.youtubemusic.source.PlaylistRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val TAG = "MediaBackgroundService"

const val CATEGORIES_CONTENT = "__youtube_musics_categories__"
const val EMPTY_CONTENT = "__empty__"

const val PLAYBACK_STATE_PLAYING_PLAYLIST = "com.yurii.youtubemusic.playback.state.playing.category.name"
const val PLAYBACK_STATE_MEDIA_ITEM = "com.yurii.youtubemusic.playback.state.media.item"
const val PLAYBACK_STATE_SESSION_ID = "com.yurii.youtubemusic.playback.state.session.id"

const val EXTRA_KEY_PLAYLIST = "com.yurii.youtubemusic.playlist"

const val FAILED_TO_LOAD_CATEGORIES = "failed_to_load_categories"
const val FAILED_TO_LOAD_MEDIA_ITEMS = "failed_to_load_media_items"
const val BROKEN_MEDIA_ITEM = "broken_media_item"
const val PLAYER_ERROR = "player_error"
const val EXTRA_EXCEPTION = "exception"

private const val VOLUME_DUCK = 0.2f
private const val VOLUME_NORMAL = 1.0f

private enum class AudioFocus {
    NoFocus,
    CanDuck,
    Focused
}

@AndroidEntryPoint
class MediaService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var audioEffectManager: AudioEffectManager

    @Inject
    lateinit var mediaLibraryDomain: MediaLibraryDomain

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var queueProvider: QueueProvider

    private val notificationManager by lazy { NotificationManager(baseContext, sessionToken!!) }

    private lateinit var mediaSession: MediaSessionCompat

    private var mediaPlayer: MediaPlayer? = null
    private var currentState = PlaybackStateCompat.STATE_NONE
    private var canPlayOnFocusGain = false
    private var currentAudioFocus = AudioFocus.NoFocus
    private val becomingNoisyReceiver = BecomingNoisyReceiver()
    private val playbackStateBuilder = PlaybackStateCompat.Builder()
    private val coroutineScopeJob = Job()
    private val coroutineScope = CoroutineScope(coroutineScopeJob)

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        updateCurrentPlaybackState()

        queueProvider.playingItemRemovedCallback = {
            if (currentState == PlaybackStateCompat.STATE_PLAYING || currentState == PlaybackStateCompat.STATE_PAUSED)
                handleStopRequest()
        }
    }

    private fun initMediaSession() {
        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            sessionIntent.putExtra(MainActivity.EXTRA_LAUNCH_FRAGMENT, MainActivity.EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
            PendingIntent.getActivity(this, 0, sessionIntent, flags)
        }

        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(applicationContext, MediaButtonReceiver::class.java)
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)

        mediaSession = MediaSessionCompat(baseContext, TAG, mediaButtonReceiver, null).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(MediaSessionCallBacks())
            setMediaButtonReceiver(pendingIntent)
            setSessionActivity(sessionActivityPendingIntent)
        }
        sessionToken = mediaSession.sessionToken
        queueProvider.mediaSession = mediaSession
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            EMPTY_CONTENT -> result.detach()
            CATEGORIES_CONTENT -> requestPlaylists(result)
            else -> requestMusicItemsByCategory(parentId, result)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            BrowserRoot(CATEGORIES_CONTENT, null)
        } else
            BrowserRoot(EMPTY_CONTENT, null)
    }

    private fun requestPlaylists(result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        coroutineScope.launch {
            result.sendResult(try {
                val playlists = playlistRepository.getPlaylists().first()
                playlists.map { it.toMediaItem() }
            } catch (error: Exception) {
                sendMediaSessionError(FAILED_TO_LOAD_CATEGORIES, error)
                null
            })
        }
    }

    private fun requestMusicItemsByCategory(patentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        coroutineScope.launch {
            result.sendResult(getMediaBrowserMediaItemsFor(patentId.toLong()))
        }
    }

    private suspend fun getMediaBrowserMediaItemsFor(playlistId: Long): List<MediaBrowserCompat.MediaItem>? {
        return try {
            val mediaItems = mediaLibraryDomain.getMediaItems(MediaItemPlaylist(playlistId, "")).first()
            mediaItems.map { it.toCompatMediaItem() }
        } catch (error: Exception) {
            sendMediaSessionError(FAILED_TO_LOAD_MEDIA_ITEMS, error)
            null
        }
    }

    private fun updateCurrentPlaybackState() = synchronized(this) {
        val extras = Bundle().apply {
            if (currentState == PlaybackStateCompat.STATE_PLAYING || currentState == PlaybackStateCompat.STATE_PAUSED) {
                putInt(PLAYBACK_STATE_SESSION_ID, getMediaPlayer().audioSessionId)
                putParcelable(PLAYBACK_STATE_MEDIA_ITEM, queueProvider.playingMediaItem)
                putParcelable(PLAYBACK_STATE_PLAYING_PLAYLIST, queueProvider.playingPlaylist)
            }
        }
        val currentPlaybackState = getCurrentPlaybackStateBuilder().apply {
            setExtras(extras)
        }.build()
        mediaSession.setPlaybackState(currentPlaybackState)
    }

    private fun sendMediaSessionError(errorEvent: String, error: Exception) =
        mediaSession.sendSessionEvent(errorEvent, Bundle().apply { putSerializable(EXTRA_EXCEPTION, error) })

    private fun getCurrentPlaybackStateBuilder(): PlaybackStateCompat.Builder {
        val position: Long = mediaPlayer?.run { currentPosition.toLong() } ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN

        return playbackStateBuilder.apply {
            setActions(getAvailableActions())
            setState(
                currentState,
                position,
                if (currentState == PlaybackStateCompat.STATE_PAUSED) 0f else 1.0f,
                SystemClock.elapsedRealtime()
            )
        }
    }

    private fun getAvailableActions(): Long {
        return if (queueProvider.playingPlaylist != null) {
            val defaultActions: Long = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_STOP

            if (currentState == PlaybackStateCompat.STATE_PLAYING) {
                defaultActions or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            } else
                defaultActions

        } else PlaybackStateCompat.ACTION_PREPARE
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
        try {
            prepareMusicFromQueue()
        } catch (error: Exception) {
            sendMediaSessionError(PLAYER_ERROR, error)
        }
    }

    private fun prepareMusicFromQueue() {
        currentState = PlaybackStateCompat.STATE_BUFFERING
        mediaSession.setMetadata(queueProvider.playingMediaItem!!.toMediaMetadataCompat())
        updateCurrentPlaybackState()
        resetOrCreateMediaPlayer()

        getMediaPlayer().apply {
            setDataSource(queueProvider.playingMediaItem!!.mediaFile.absolutePath)
            prepareAsync()
        }
    }

    private fun playMediaPlayer() = synchronized(this) {
        currentState = PlaybackStateCompat.STATE_PLAYING
        canPlayOnFocusGain = false
        getMediaPlayer().apply {
            start()
            audioEffectManager.setSession(audioSessionId)
            coroutineScope.launch { audioEffectManager.applyCurrentAffects() }
        }
        notificationManager.showPlayingNotification()
        startForeground(NotificationManager.NOTIFICATION_ID, notificationManager.getCurrentNotification())
        updateCurrentPlaybackState()
    }

    private fun pauseMediaPlayer() = synchronized(this) {
        currentState = PlaybackStateCompat.STATE_PAUSED
        getMediaPlayer().pause()
        notificationManager.showPauseNotification()
        stopForegroundCompat(false)
        updateCurrentPlaybackState()
    }

    private fun handleStopRequest() {
        currentState = PlaybackStateCompat.STATE_STOPPED

        getMediaPlayer().apply {
            reset()
            release()
            mediaPlayer = null
        }

        stopForegroundCompat(true)
        giveUpAudioFocus()
        updateCurrentPlaybackState()
        stopSelf()
    }

    private inner class MediaSessionCallBacks : MediaSessionCompat.Callback() {
        private val preparePlayerLock = Mutex()

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)
            // Not implemented yet
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

            coroutineScope.launch(Dispatchers.IO) {
                preparePlayerLock.withLock {
                    val playlist = extras?.parcelable(EXTRA_KEY_PLAYLIST) ?: MediaItemPlaylist.ALL
                    queueProvider.prepareQueue(playlist)
                    queueProvider.play(mediaId)
                    registerReceiver(becomingNoisyReceiver, becomingNoisyReceiver.becomingNoisyIntent)
                    handlePlayMusicQueue()
                }
            }
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
            queueProvider.skipToNext()
            handlePlayMusicQueue()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            queueProvider.skipToPrevious()
            handlePlayMusicQueue()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            mediaPlayer?.seekTo(pos.toInt())
            updateCurrentPlaybackState()
        }
    }

    private inner class MediaPlayerCallBacks : MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
        override fun onPrepared(mp: MediaPlayer?) {
            playMediaPlayer()
        }

        override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
            sendMediaSessionError(PLAYER_ERROR, Exception("Player error: $what"))
            return true
        }

        override fun onCompletion(mp: MediaPlayer?) {
            coroutineScope.launch {
                queueProvider.next()
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

    override fun onDestroy() {
        super.onDestroy()
        queueProvider.releaseQueue()
        coroutineScopeJob.cancel()
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