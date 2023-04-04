package com.yurii.youtubemusic.screens.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentPlayerControlPanelBinding
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.services.media.PlaybackState
import com.yurii.youtubemusic.ui.startValueAnimation
import com.yurii.youtubemusic.utilities.setUniqueAnimatedDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs


@AndroidEntryPoint
class PlayerControlPanelFragment : Fragment(R.layout.fragment_player_control_panel) {
    private val viewModel: PlayerControllerViewModel by viewModels()
    private val binding: FragmentPlayerControlPanelBinding by viewBinding()

    private var clickX = 0f
    private var hasBeenClicked = false
    private var displayWidth = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        signDisplaySize()

        binding.actionButton.setOnClickListener { viewModel.pauseOrPlay() }

        binding.container.setOnTouchListener { _, event ->
            handleCardContainerTouchEvent(event)
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observePlaybackState() }
                launch { observePlayingPosition() }
            }
        }
    }

    private suspend fun observePlayingPosition() = viewModel.currentPosition.collectLatest {
        binding.currentTimePosition = it
    }

    private suspend fun observePlaybackState() = viewModel.playbackState.collectLatest {
        when (it) {
            PlaybackState.None -> swipeViewAway()
            is PlaybackState.Playing -> setMediaItem(it.mediaItem, !it.isPaused, it.playlist)
        }
    }


    private fun setMediaItem(mediaItem: MediaItem, isPlaying: Boolean, playlist: MediaItemPlaylist?) {
        binding.apply {
            this.mediaItem = mediaItem
            thumbnail.load(mediaItem.thumbnail)
            if (isPlaying)
                actionButton.setUniqueAnimatedDrawable(R.drawable.anim_from_pause_to_play_48dp)
            else
                actionButton.setUniqueAnimatedDrawable(R.drawable.anim_from_play_to_pause_48dp)
            playingPlaylist = playlist
            if (!container.isVisible)
                showMusicPlayerPanel()
        }
    }

    private fun signDisplaySize() {
        DisplayMetrics().also {
            activity?.windowManager?.defaultDisplay?.getMetrics(it)
            displayWidth = it.widthPixels
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

    private fun handleStopRequest() {
        swipeViewAway()
        viewModel.stopPlaying()
    }

    private fun returnViewToCenter() =
        startValueAnimation(binding.container.x, 0f) { binding.container.x = it }

    private fun swipeViewAway() {
        if (!binding.container.isVisible)
            return
        binding.container.apply {
            val end = if (x > 0) x + width else x - width
            startValueAnimation(x, end, onEnd = { isVisible = false }) { x = it }
        }

        vibrationEffect()
    }

    private fun vibrationEffect() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        else
           vibrator.vibrate(40)
    }
}