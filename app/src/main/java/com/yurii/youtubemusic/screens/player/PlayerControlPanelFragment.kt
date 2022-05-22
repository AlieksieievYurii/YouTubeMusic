package com.yurii.youtubemusic.screens.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.media.session.PlaybackStateCompat
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentPlayerControlPanelBinding
import com.yurii.youtubemusic.screens.saved.service.PLAYBACK_STATE_PLAYING_CATEGORY_NAME
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.ui.startValueAnimation
import com.yurii.youtubemusic.utilities.Injector
import kotlin.math.abs


class PlayerControlPanelFragment : Fragment(R.layout.fragment_player_control_panel) {
    private val viewModel: PlayerControllerViewModel by viewModels { Injector.providePlayerControllerViewModel(requireContext()) }
    private val binding: FragmentPlayerControlPanelBinding by viewBinding()

    private var clickX = 0f
    private var hasBeenClicked = false
    private var displayWidth = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        signDisplaySize()
        initView()
    }

    private fun signDisplaySize() {
        DisplayMetrics().also {
            activity?.windowManager?.defaultDisplay?.getMetrics(it)
            displayWidth = it.widthPixels
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        viewModel.playingNow.observe(viewLifecycleOwner) { mediaItem: MediaMetaData? ->
            binding.mediaItem = mediaItem
            if (mediaItem != null && !binding.container.isVisible)
                showMusicPlayerPanel()
        }

        viewModel.currentPlaybackState.observe(viewLifecycleOwner) { playback ->
            binding.isPlayingNow = viewModel.isPlaying()
            playback.extras?.getString(PLAYBACK_STATE_PLAYING_CATEGORY_NAME)?.run { binding.playingCategory = this }

            if (playback.state == PlaybackStateCompat.STATE_STOPPED)
                swipeViewAway()
        }

        viewModel.currentProgressTime.observe(viewLifecycleOwner)  { binding.currentTimePosition = it }

        binding.actionButton.setOnClickListener { viewModel.onPauseOrPlay() }

        binding.container.setOnTouchListener { _, event ->
            handleCardContainerTouchEvent(event)
            false
        }
    }

    private fun openPlayerActivity() {
        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }

    private fun handleCardContainerTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clickX = event.x
                hasBeenClicked = true
            }
            MotionEvent.ACTION_MOVE -> {
                hasBeenClicked = false
                binding.container.x = binding.container.x + (event.x - clickX)
            }

            MotionEvent.ACTION_UP -> {
                when {
                    hasBeenClicked -> openPlayerActivity()
                    abs(binding.container.x) >= binding.container.width / 2 -> handleStopRequest()
                    else -> returnViewToCenter()
                }
            }
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
        vibrationEffect()
    }

    private fun vibrationEffect() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        else
            vibrator.vibrate(40)
    }
}