package com.yurii.youtubemusic.screens.player

import androidx.annotation.IntRange
import androidx.lifecycle.*
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import java.lang.IllegalStateException

class PlayerControllerViewModel(private val mediaServiceConnection: MediaServiceConnection) : ViewModel() {

    val playbackState = mediaServiceConnection.playbackState

    fun pauseOrPlay() {
        when (playbackState.value) {
            PlaybackState.None -> TODO()
            is PlaybackState.Paused -> mediaServiceConnection.resume()
            is PlaybackState.Playing -> mediaServiceConnection.pause()
        }
    }

    fun moveToNextTrack() = mediaServiceConnection.skipToNextTrack()

    fun moveToPreviousTrack() = mediaServiceConnection.skipToPreviousTrack()


    fun onSeek(@IntRange(from = 0, to = 1000) value: Int) {
        //TODO implement on seek
    }

    fun stopPlaying() {

    }


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