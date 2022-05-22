package com.yurii.youtubemusic.screens.player

import android.app.Application
import android.content.Context
import android.os.Handler
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.IntRange
import androidx.lifecycle.*
import com.yurii.youtubemusic.screens.saved.service.MusicServiceConnection
import com.yurii.youtubemusic.screens.saved.service.NOTHING_PLAYING
import com.yurii.youtubemusic.models.MediaMetaData
import java.lang.IllegalStateException

class PlayerControllerViewModel(application: Application, val musicServiceConnection: MusicServiceConnection) :
    AndroidViewModel(application) {
    private val timeUpdater = TimeUpdater()

    val playingNow: LiveData<MediaMetaData?> = Transformations.map(musicServiceConnection.nowPlaying) {
        if (it != NOTHING_PLAYING)
            MediaMetaData.createFrom(it)
        else
            null
    }

    val currentPlaybackState: LiveData<PlaybackStateCompat> = Transformations.map(musicServiceConnection.playbackState) { playback ->
        updateTimeCounter(playback.state)
        playback
    }

    private val _currentProgressTime: MutableLiveData<Long> = MutableLiveData()
    val currentProgressTime: LiveData<Long> = _currentProgressTime

    fun isPlaying(): Boolean = currentPlaybackState.value?.state?.run {
        this == PlaybackStateCompat.STATE_PLAYING || this == PlaybackStateCompat.STATE_BUFFERING
    } ?: false

    fun onPauseOrPlay() {
        if(isPlaying()) pausePlaying() else continuePlaying()
    }

    fun stopPlaying() = musicServiceConnection.transportControls.stop()

    private fun pausePlaying() = musicServiceConnection.transportControls.pause()

    private fun continuePlaying() = musicServiceConnection.transportControls.play()

    fun moveToNextTrack() = musicServiceConnection.transportControls.skipToNext()

    fun moveToPreviousTrack() = musicServiceConnection.transportControls.skipToPrevious()


    fun onSeek(@IntRange(from = 0, to = 1000) value: Int) {
        val duration = playingNow.value?.duration ?: 0
        val pos = value * duration / 1000
        musicServiceConnection.transportControls.seekTo(pos)
    }

    private fun updateTimeCounter(playbackState: Int) {
        when (playbackState) {
            PlaybackStateCompat.STATE_BUFFERING -> timeUpdater.stop()
            PlaybackStateCompat.STATE_PLAYING -> timeUpdater.start()
            PlaybackStateCompat.STATE_PAUSED -> timeUpdater.stop()
        }
    }

    private inner class TimeUpdater : Runnable {
        private val handler = Handler()
        override fun run() {
            musicServiceConnection.requestCurrentMediaTimePosition { a ->
                _currentProgressTime.postValue(a)
                handler.postDelayed(this, 1000L)
            }
        }

        fun start() = run()
        fun stop() = handler.removeCallbacks(this)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerControllerViewModel::class.java))
                return PlayerControllerViewModel(context as Application, musicServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from PlayerBottomControllerViewModel class")
        }
    }
}