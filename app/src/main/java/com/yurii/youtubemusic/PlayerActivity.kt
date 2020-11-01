package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.yurii.youtubemusic.databinding.ActivityPlayerBinding
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.viewmodels.PlayerControllerViewModel

class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerControllerViewModel by viewModels {
        Injector.providePlayerControllerViewModel(this)
    }

    private lateinit var binding: ActivityPlayerBinding

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

        viewModel.currentProgressTime.observe(this, Observer {
            binding.currentTimePosition = it
            binding.seekBar.progress = (it * 1000 / viewModel.playingNow.value!!.duration).toInt()
        })

        binding.moveToNext.setOnClickListener { viewModel.moveToNextTrack() }
        binding.moveToPrevious.setOnClickListener { viewModel.moveToPreviousTrack() }
    }
}