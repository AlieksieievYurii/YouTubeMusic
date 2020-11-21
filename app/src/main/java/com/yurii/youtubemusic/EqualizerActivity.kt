package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.yurii.youtubemusic.databinding.ActivityEqualizerBinding
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
        supportActionBar!!.title = getString(R.string.label_audio_effects)

        binding.enableEqualizer.setOnCheckedChangeListener { _, isChecked ->
            viewModel.audioEffectManager.setEnableEqualizer(isChecked)
            binding.equalizer.setEnable(isChecked)
        }
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
            if (fromUser)
                viewModel.audioEffectManager.setBandLevel(bandId, level)
        }

        viewModel.audioEffectManager.data.also { audioEffectsData ->
            binding.enableEqualizer.isChecked = audioEffectsData.enableEqualizer
            binding.enableBassBoost.isChecked = audioEffectsData.enableBassBoost
            binding.enableVirtualizer.isChecked = audioEffectsData.enableVirtualizer

            binding.bassBoost.apply {
                setValue(audioEffectsData.bassBoost)
                setEnable(audioEffectsData.enableBassBoost)
            }
            binding.virtualizer.apply {
                setValue(audioEffectsData.virtualizer)
                setEnable(audioEffectsData.enableVirtualizer)
            }

            binding.equalizer.apply {
                setEnable(audioEffectsData.enableEqualizer)
                setBands(audioEffectsData.bands)
                minValue = audioEffectsData.lowestBandLevel.toInt()
                maxValue = audioEffectsData.highestBandLevel.toInt()
                draw()
                setBandSettings(audioEffectsData.bandsLevels)
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