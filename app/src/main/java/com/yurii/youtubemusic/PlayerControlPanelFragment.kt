package com.yurii.youtubemusic

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.yurii.youtubemusic.databinding.FragmentPlayerControlPanelBinding
import com.yurii.youtubemusic.services.mediaservice.PLAYBACK_STATE_PLAYING_CATEGORY_NAME
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.Injector.providePlayerBottomControllerViewModel
import com.yurii.youtubemusic.utilities.TimeCounter
import com.yurii.youtubemusic.utilities.startValueAnimation
import com.yurii.youtubemusic.viewmodels.PlayerBottomControllerViewModel
import kotlin.math.abs


class PlayerControlPanelFragment : Fragment() {
    private val viewModel: PlayerBottomControllerViewModel by viewModels {
        providePlayerBottomControllerViewModel(requireContext())
    }

    private lateinit var binding: FragmentPlayerControlPanelBinding
    private val timeCounter = TimeCounter { binding.currentTimePosition = it }
    private var clickX = 0f
    private var displayWidth = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_player_control_panel, container, false)
        signDisplaySize()
        initView()
        return binding.root
    }

    private fun signDisplaySize() {
        DisplayMetrics().also {
            activity?.windowManager?.defaultDisplay?.getMetrics(it)
            displayWidth = it.widthPixels
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        viewModel.playingNow.observe(viewLifecycleOwner, Observer { mediaItem: MediaMetaData? ->
            binding.mediaItem = mediaItem
            if (mediaItem != null && !binding.container.isVisible)
                showMusicPlayerPanel()
        })

        viewModel.currentPlaybackState.observe(viewLifecycleOwner, Observer { playback ->
            binding.isPlayingNow = viewModel.isPlaying()
            playback.extras?.getString(PLAYBACK_STATE_PLAYING_CATEGORY_NAME)?.run { binding.playingCategory = this }

            updateTimeCounterState(playback)
            if (playback.state == PlaybackStateCompat.STATE_STOPPED)
                swipeViewAway()
        })

        binding.actionButton.setOnClickListener {
            if (viewModel.isPlaying())
                viewModel.pausePlaying()
            else
                viewModel.continuePlaying()
        }

        binding.container.setOnTouchListener { _, event ->
            handleCardContainerTouchEvent(event)
            false
        }
    }

    private fun updateTimeCounterState(playbackState: PlaybackStateCompat) {
        when (playbackState.state) {
            PlaybackStateCompat.STATE_BUFFERING -> timeCounter.reset()
            PlaybackStateCompat.STATE_PLAYING -> timeCounter.start(playbackState.position)
            PlaybackStateCompat.STATE_PAUSED -> timeCounter.stop()
        }
    }

    private fun handleCardContainerTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> clickX = event.x
            MotionEvent.ACTION_MOVE -> binding.container.x = binding.container.x + (event.x - clickX)
            MotionEvent.ACTION_UP -> if (abs(binding.container.x) >= binding.container.width / 2) handleStopRequest() else returnViewToCenter()
        }
    }

    private fun showMusicPlayerPanel() {
        binding.container.isVisible = false
        binding.container.x = -displayWidth.toFloat()
        binding.container.isVisible = true
        startValueAnimation(binding.container.x, 0f) { binding.container.x = it }
    }

    private fun hideMusicPlayerPanel() {
        binding.container.isVisible = false
    }

    private fun handleStopRequest() {
        swipeViewAway()
        viewModel.stopPlaying()
    }

    private fun returnViewToCenter() = startValueAnimation(binding.container.x, 0f) { binding.container.x = it }

    private fun swipeViewAway() {
        val end = if (binding.container.x > 0) binding.container.x + binding.container.width else binding.container.x - binding.container.width
        startValueAnimation(binding.container.x, end, onEnd = { hideMusicPlayerPanel() }) { binding.container.x = it }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeCounter.stop()
    }
}