package com.yurii.youtubemusic.screens.player

import androidx.annotation.IntRange
import androidx.lifecycle.*
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerControllerViewModel @Inject constructor (private val mediaServiceConnection: MediaServiceConnection) : ViewModel() {

    private var timerJob: Job? = null

    private val _currentPosition: MutableStateFlow<Long> = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    val playbackState = mediaServiceConnection.playbackState

    val isQueueLooped = mediaServiceConnection.isQueueLooped

    val isShuffled = mediaServiceConnection.isQueueShuffle

    init {
        viewModelScope.launch {
            playbackState.collect {
                when (it) {
                    PlaybackState.None -> timerJob?.cancel()
                    is PlaybackState.Playing -> {
                        _currentPosition.value = it.currentPosition
                        if (it.isPaused) timerJob?.cancel() else runTicker()
                    }
                }
            }
        }
    }

    private fun runTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _currentPosition.update {
                    it + 1000
                }
            }
        }
    }

    fun loopStateClick() {
        viewModelScope.launch {
            mediaServiceConnection.setLoopState(!isQueueLooped.first())
        }
    }

    fun shuffleStateClick() {
        viewModelScope.launch {
            mediaServiceConnection.setShuffleState(!isShuffled.first())
        }
    }

    fun pauseOrPlay() {
        when (val it = playbackState.value) {
            PlaybackState.None -> {
                // Ignore
            }
            is PlaybackState.Playing -> if (it.isPaused) mediaServiceConnection.resume() else mediaServiceConnection.pause()
        }
    }

    fun moveToNextTrack() = mediaServiceConnection.skipToNextTrack()

    fun moveToPreviousTrack() = mediaServiceConnection.skipToPreviousTrack()


    fun seekTo(@IntRange(from = 0, to = 1000) value: Int) {
        timerJob?.cancel()
        (playbackState.value as? PlaybackState.Playing)?.let {
            mediaServiceConnection.seekTo(value * it.mediaItem.durationInMillis / 1000)
        }
    }

    fun getCurrentMappedPosition(): Int =
        (playbackState.value as? PlaybackState.Playing)?.let { (_currentPosition.value * 1000 / it.mediaItem.durationInMillis).toInt() } ?: 0

    fun stopPlaying() = mediaServiceConnection.stop()
}