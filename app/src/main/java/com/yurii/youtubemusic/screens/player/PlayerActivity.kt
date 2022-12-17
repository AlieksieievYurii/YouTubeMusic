package com.yurii.youtubemusic.screens.player

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
    private var playingMediaItemDuration: Long? = null

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

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.i("TEST", seekBar.progress.toString())
            }
        })
    }

    private suspend fun observePlayingPosition() = viewModel.currentPosition.collectLatest {
        binding.currentTimePosition = it
        playingMediaItemDuration?.let { mediaItemDuration ->
            binding.seekBar.progress = (it * 1000 / mediaItemDuration).toInt()
        }
    }

    private suspend fun observePlaybackState() = viewModel.playbackState.collectLatest {
        when (it) {
            PlaybackState.None -> TODO()
            is PlaybackState.Paused -> {
                binding.mediaItem = it.mediaItem
                binding.isPlayingNow = false
                playingMediaItemDuration = it.mediaItem.durationInMillis
            }
            is PlaybackState.Playing -> {
                binding.mediaItem = it.mediaItem
                binding.isPlayingNow = true
                playingMediaItemDuration = it.mediaItem.durationInMillis
            }
        }
    }
}