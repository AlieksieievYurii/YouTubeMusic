package com.yurii.youtubemusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.yurii.youtubemusic.databinding.FragmentPlayerControlPanelBinding
import com.yurii.youtubemusic.utilities.Injector.providePlayerBottomControllerViewModel
import com.yurii.youtubemusic.viewmodels.PlayerBottomControllerViewModel

class PlayerControlPanelFragment : Fragment() {
    private val viewModel: PlayerBottomControllerViewModel by viewModels {
        providePlayerBottomControllerViewModel(requireContext())
    }
    private lateinit var binding: FragmentPlayerControlPanelBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_player_control_panel, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        viewModel.playingNow.observe(viewLifecycleOwner, Observer { binding.mediaItem = it })

        viewModel.isNowPlaying.observe(viewLifecycleOwner, Observer { binding.isPlayingNow = it })

        binding.actionButton.setOnClickListener {
            if (viewModel.isNowPlaying.value!!)
                viewModel.pausePlaying()
            else
                viewModel.continuePlaying()
        }
    }
}