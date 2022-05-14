package com.yurii.youtubemusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.yurii.youtubemusic.databinding.ActivityPlayerBinding
import com.yurii.youtubemusic.screens.equalizer.EqualizerActivity
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.viewmodels.PlayerControllerViewModel

class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerControllerViewModel by viewModels {
        Injector.providePlayerControllerViewModel(this)
    }

    private lateinit var binding: ActivityPlayerBinding
    private var isStartChangingSeek: Boolean = false
    private var seekProgress: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)

        viewModel.playingNow.observe(this, Observer { binding.mediaItem = it })

        viewModel.currentPlaybackState.observe(this, Observer { binding.isPlayingNow = viewModel.isPlaying() })

        binding.actionButton.setOnClickListener {
            if (viewModel.isPlaying())
                viewModel.pausePlaying()
            else
                viewModel.continuePlaying()
        }

        binding.openAudioEffects.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }

        viewModel.currentProgressTime.observe(this, Observer {
            if (!isStartChangingSeek) {
                binding.currentTimePosition = it
                binding.seekBar.progress = (it * 1000 / viewModel.playingNow.value!!.duration).toInt()
            }
        })

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