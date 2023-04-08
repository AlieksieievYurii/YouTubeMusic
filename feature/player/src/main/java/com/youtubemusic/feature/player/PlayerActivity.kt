package com.youtubemusic.feature.player

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.util.TypedValue
import android.viewbinding.library.activity.viewBinding
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.youtubemusic.core.common.setTint
import com.youtubemusic.core.common.setUniqueAnimatedDrawable
import com.youtubemusic.core.player.PlaybackState
import com.youtubemusic.feature.equalizer.EqualizerActivity
import com.youtubemusic.feature.player.databinding.ActivityPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    private val viewModel: PlayerControllerViewModel by viewModels()
    private val binding: ActivityPlayerBinding by viewBinding()
    private var isSeekBarChanging = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observePlaybackState() }
                launch { observePlayingPosition() }
                launch { observeLoopMode() }
                launch { observeShuffleMode() }
            }
        }

        binding.apply {
            actionButton.setOnClickListener { viewModel.pauseOrPlay() }
            openAudioEffects.setOnClickListener { startActivity(Intent(applicationContext, EqualizerActivity::class.java)) }
            moveToNext.setOnClickListener { viewModel.moveToNextTrack() }
            moveToPrevious.setOnClickListener { viewModel.moveToPreviousTrack() }
            loopMode.setOnClickListener { viewModel.loopStateClick() }
            shuffleMode.setOnClickListener { viewModel.shuffleStateClick() }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Nothing
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekBarChanging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekBarChanging = false
                viewModel.seekTo(seekBar.progress)
            }
        })
    }

    private suspend fun observeLoopMode() = viewModel.isQueueLooped.collect { isLooped ->
        binding.loopMode.setTint(
            if (isLooped) getAttrColor(android.R.attr.colorAccent) else ContextCompat.getColor(
                this,
                android.R.color.darker_gray
            )
        )
    }

    private suspend fun observeShuffleMode() = viewModel.isShuffled.collect { isShuffled ->
        binding.shuffleMode.setTint(
            if (isShuffled) getAttrColor(android.R.attr.colorAccent) else ContextCompat.getColor(
                this,
                android.R.color.darker_gray
            )
        )
    }

    private suspend fun observePlayingPosition() = viewModel.currentPosition.collectLatest {
        binding.currentTimePosition = it
        if (!isSeekBarChanging)
            binding.seekBar.progress = viewModel.getCurrentMappedPosition()
    }

    private suspend fun observePlaybackState() = viewModel.playbackState.collectLatest {
        when (it) {
            PlaybackState.None -> {
                //Nothing
            }
            is PlaybackState.Playing -> binding.apply {
                thumbnail.load(it.mediaItem.thumbnail)
                mediaItem = it.mediaItem
                if (it.isPaused)
                    actionButton.setUniqueAnimatedDrawable(com.youtubemusic.core.common.R.drawable.anim_from_play_to_pause_48dp)
                else
                    actionButton.setUniqueAnimatedDrawable(com.youtubemusic.core.common.R.drawable.anim_from_pause_to_play_48dp)
            }
        }
    }
}

fun Context.getAttrColor(attr: Int): Int {
    val typedValue = TypedValue()
    val a: TypedArray = obtainStyledAttributes(typedValue.data, intArrayOf(attr))
    val color = a.getColor(0, 0)
    a.recycle()
    return color
}