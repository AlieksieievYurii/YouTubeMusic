package com.yurii.youtubemusic.screens.player

import androidx.annotation.IntRange
import androidx.lifecycle.*
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class PlayerControllerViewModel(private val mediaServiceConnection: MediaServiceConnection) : ViewModel() {

    private var timerJob: Job? = null

    private val _currentPosition: MutableStateFlow<Long> = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    val playbackState = mediaServiceConnection.playbackState

    var playingMediaItemDuration: Long? = null
        private set

    init {
        viewModelScope.launch {
            playbackState.collect {
                when (it) {
                    PlaybackState.None -> timerJob?.cancel()
                    is PlaybackState.Paused -> {
                        playingMediaItemDuration = it.mediaItem.durationInMillis
                        timerJob?.cancel()
                    }
                    is PlaybackState.Playing -> {
                        playingMediaItemDuration = it.mediaItem.durationInMillis
                        runTimer(it.currentPosition)
                    }
                }
            }
        }
    }

    private fun runTimer(startTimeInMillis: Long) {
        _currentPosition.value = startTimeInMillis
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _currentPosition.value += 1000
            }
        }
    }

    fun pauseOrPlay() {
        when (playbackState.value) {
            PlaybackState.None -> {
                // Ignore
            }
            is PlaybackState.Paused -> mediaServiceConnection.resume()
            is PlaybackState.Playing -> mediaServiceConnection.pause()
        }
    }

    fun moveToNextTrack() = mediaServiceConnection.skipToNextTrack()

    fun moveToPreviousTrack() = mediaServiceConnection.skipToPreviousTrack()


    fun seekTo(@IntRange(from = 0, to = 1000) value: Int) {
        timerJob?.cancel()
        val targetPosition = playingMediaItemDuration?.let { value * it / 1000 } ?: 0
        playingMediaItemDuration?.let {
            mediaServiceConnection.seekTo(targetPosition)
        }
    }

    fun getCurrentMappedPosition(): Int = playingMediaItemDuration?.let { (_currentPosition.value * 1000 / it).toInt() } ?: 0

    fun stopPlaying() = mediaServiceConnection.stop()

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mediaServiceConnection: MediaServiceConnection) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerControllerViewModel::class.java))
                return PlayerControllerViewModel(mediaServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from PlayerBottomControllerViewModel class")
        }
    }
}