package com.yurii.youtubemusic

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.yurii.youtubemusic.databinding.FragmentPlayerControlPanelBinding
import com.yurii.youtubemusic.utilities.Injector.providePlayerBottomControllerViewModel
import com.yurii.youtubemusic.utilities.TimeCounter
import com.yurii.youtubemusic.viewmodels.PlayerBottomControllerViewModel

class PlayerControlPanelFragment : Fragment() {
    private val viewModel: PlayerBottomControllerViewModel by viewModels {
        providePlayerBottomControllerViewModel(requireContext())
    }
    private lateinit var binding: FragmentPlayerControlPanelBinding
    private val timeCounter = TimeCounter { binding.currentTimePosition = it }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_player_control_panel, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        viewModel.playingNow.observe(viewLifecycleOwner, Observer { mediaItem -> binding.mediaItem = mediaItem })

        viewModel.currentPlaybackState.observe(viewLifecycleOwner, Observer { playback ->
            binding.isPlayingNow = viewModel.isPlaying()

            when (playback.state) {
                PlaybackStateCompat.STATE_BUFFERING -> timeCounter.reset()
                PlaybackStateCompat.STATE_PLAYING -> timeCounter.start(playback.position)
                PlaybackStateCompat.STATE_PAUSED -> timeCounter.stop()
            }
        })

        binding.actionButton.setOnClickListener {
            if (viewModel.isPlaying())
                viewModel.pausePlaying()
            else
                viewModel.continuePlaying()
        }
    }
}