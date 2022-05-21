package com.yurii.youtubemusic.screens.player

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import android.widget.SeekBar
import androidx.activity.viewModels
import com.yurii.youtubemusic.databinding.ActivityPlayerBinding
import com.yurii.youtubemusic.screens.equalizer.EqualizerActivity
import com.yurii.youtubemusic.utilities.Injector

class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerControllerViewModel by viewModels { Injector.providePlayerControllerViewModel(this) }

    private val binding: ActivityPlayerBinding by viewBinding()
    private var isStartChangingSeek: Boolean = false
    private var seekProgress: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.playingNow.observe(this) { binding.mediaItem = it }
        viewModel.currentPlaybackState.observe(this) { binding.isPlayingNow = viewModel.isPlaying() }

        binding.actionButton.setOnClickListener { viewModel.onPauseOrPlay() }

        binding.openAudioEffects.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }

        viewModel.currentProgressTime.observe(this) {
            if (!isStartChangingSeek) {
                binding.currentTimePosition = it
                binding.seekBar.progress = (it * 1000 / viewModel.playingNow.value!!.duration).toInt()
            }
        }

        binding.moveToNext.setOnClickListener { viewModel.moveToNextTrack() }
        binding.moveToPrevious.setOnClickListener { viewModel.moveToPreviousTrack() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isStartChangingSeek) {
                    binding.currentTimePosition = progress * viewModel.playingNow.value!!.duration / 1000
                    seekProgress = progress
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
}