package com.yurii.youtubemusic.screens.player

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yurii.youtubemusic.databinding.ActivityPlayerBinding
import com.yurii.youtubemusic.screens.equalizer.EqualizerActivity
import com.yurii.youtubemusic.services.media.PlaybackState
import com.yurii.youtubemusic.utilities.Injector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerControllerViewModel by viewModels { Injector.providePlayerControllerViewModel(application) }
    private val binding: ActivityPlayerBinding by viewBinding()
    private var isStartChangingSeek: Boolean = false
    private var seekProgress: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observePlaybackState() }
                launch { observePlayingPosition() }
            }
        }

        binding.actionButton.setOnClickListener { viewModel.pauseOrPlay() }

        binding.openAudioEffects.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }


        binding.moveToNext.setOnClickListener { viewModel.moveToNextTrack() }
        binding.moveToPrevious.setOnClickListener { viewModel.moveToPreviousTrack() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isStartChangingSeek) {

                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isStartChangingSeek = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.onSeek(seekProgress)
                isStartChangingSeek = false
            }
        })
    }

    private suspend fun observePlayingPosition() = viewModel.currentPosition.collectLatest {
        binding.currentTimePosition = it
    }

    private suspend fun observePlaybackState() = viewModel.playbackState.collectLatest {
        when(it) {
            PlaybackState.None -> TODO()
            is PlaybackState.Paused -> {
                binding.mediaItem = it.mediaItem
                binding.isPlayingNow = false
            }
            is PlaybackState.Playing -> {
                binding.mediaItem = it.mediaItem
                binding.isPlayingNow = true
            }
        }
    }
}