package com.yurii.youtubemusic.viewmodels

import android.app.Application
import android.content.Context
import android.os.Handler
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.*
import com.yurii.youtubemusic.services.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.services.mediaservice.NOTHING_PLAYING
import com.yurii.youtubemusic.models.MediaMetaData
import java.lang.IllegalStateException

class PlayerControllerViewModel(application: Application, private val musicServiceConnection: MusicServiceConnection) :
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

    fun stopPlaying() = musicServiceConnection.transportControls.stop()

    fun pausePlaying() = musicServiceConnection.transportControls.pause()

    fun continuePlaying() = musicServiceConnection.transportControls.play()

    fun moveToNextTrack() = musicServiceConnection.transportControls.skipToNext()

    fun moveToPreviousTrack() = musicServiceConnection.transportControls.skipToPrevious()

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
}

@Suppress("UNCHECKED_CAST")
class PlayerBottomControllerFactory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerControllerViewModel::class.java))
            return PlayerControllerViewModel(context as Application, musicServiceConnection) as T
        throw IllegalStateException("Given the model class is not assignable from PlayerBottomControllerViewModel class")
    }
}