package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.yurii.youtubemusic.databinding.ActivityEqualizerBinding
import com.yurii.youtubemusic.ui.EqualizerView
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.viewmodels.EqualizerViewModel

class EqualizerActivity : AppCompatActivity() {
    private val viewModel: EqualizerViewModel by viewModels {
        Injector.provideEqualizerViewModel(this)
    }

    private lateinit var binding: ActivityEqualizerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_equalizer)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Audio Effects"

        binding.enableEqualizer.setOnCheckedChangeListener { _, isChecked -> viewModel.audioEffectManager.setEnableEqualizer(isChecked) }
        binding.enableBassBoost.setOnCheckedChangeListener { _, isChecked ->
            binding.bassBoost.setEnable(isChecked)
            viewModel.audioEffectManager.setEnableBassBoost(isChecked)
        }
        binding.enableVirtualizer.setOnCheckedChangeListener { _, isChecked ->
            binding.virtualizer.setEnable(isChecked)
            viewModel.audioEffectManager.setEnableVirtualizer(isChecked)
        }

        binding.bassBoost.listener = { viewModel.audioEffectManager.setBassBoost(it) }
        binding.virtualizer.listener = { viewModel.audioEffectManager.setVirtualizer(it) }

        binding.equalizer.setBandListener { bandId, level, fromUser ->
            Log.i("TEST", "FromUser: $fromUser; Level: ${level}")
            viewModel.audioEffectManager.setBandLevel(bandId, level)
        }

        viewModel.audioEffectManager.data.also {
            binding.enableEqualizer.isChecked = it.enableEqualizer
            binding.enableBassBoost.isChecked = it.enableBassBoost
            binding.enableVirtualizer.isChecked = it.enableVirtualizer

            binding.bassBoost.apply {
                setValue(it.bassBoost)
                setEnable(it.enableBassBoost)
            }
            binding.virtualizer.apply {
                setValue(it.virtualizer)
                setEnable(it.enableVirtualizer)
            }

            binding.equalizer.apply {
                setBands(it.bands)
                minValue = it.lowestBandLevel.toInt()
                maxValue = it.highestBandLevel.toInt()
                draw()
                setBandSettings(it.bandsLevels)
            }
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.audioEffectManager.saveChanges()
    }
}