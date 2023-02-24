package com.yurii.youtubemusic.screens.equalizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityEqualizerBinding
import com.yurii.youtubemusic.models.EqualizerData
import com.yurii.youtubemusic.ui.EqualizerView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EqualizerActivity : AppCompatActivity() {
    private val viewModel: EqualizerViewModel by viewModels()
    private val binding: ActivityEqualizerBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.label_audio_effects)
        }

        initVirtualizerState()
        initBassBoostState()
        initEqualizerData()
    }

    private fun initEqualizerData() {
        viewModel.getEqualizerData().let {
            binding.equalizer.apply {
                initEqualizer(it.lowestBandLevel.toInt(), it.highestBandLevel.toInt(), it.listOfCenterFreq)
                setBandLevels(it.bandsLevels)
                setEnable(it.isEnabled)
            }
            binding.selectPresets.apply {
                setOnClickListener { openDialogToSelectPreset() }
                isEnabled = it.isEnabled
                text = if (it.currentPreset == EqualizerData.CUSTOM_PRESET_ID)
                    getText(R.string.label_custom)
                else
                    viewModel.getPresetName(it.currentPreset)
            }
            binding.enableEqualizer.isChecked = it.isEnabled
        }
        setListenersForEqualizer()
    }

    private fun initBassBoostState() {
        viewModel.getBassBoostData().let {
            binding.apply {
                bassBoost.setEnable(it.isEnabled)
                bassBoost.value = it.value
                enableBassBoost.isChecked = it.isEnabled
            }
        }
        setListenersForBoostBass()
    }

    private fun initVirtualizerState() {
        viewModel.getVirtualizerData().let {
            binding.apply {
                virtualizer.setEnable(it.isEnabled)
                virtualizer.value = it.value
                enableVirtualizer.isChecked = it.isEnabled
            }
        }
        setListenersForVirtualizer()
    }

    private fun setListenersForBoostBass() = binding.apply {
        enableBassBoost.setOnCheckedChangeListener { _, isChecked ->
            bassBoost.setEnable(isChecked)
            viewModel.setBassBoost(isChecked, bassBoost.value)
        }
        bassBoost.listener = { viewModel.setBassBoost(enableBassBoost.isChecked, it) }
    }

    private fun setListenersForVirtualizer() = binding.apply {
        enableVirtualizer.setOnCheckedChangeListener { _, isChecked ->
            virtualizer.setEnable(isChecked)
            viewModel.setVirtualizer(isChecked, virtualizer.value)
        }
        virtualizer.listener = { viewModel.setVirtualizer(enableVirtualizer.isChecked, it) }
    }

    private fun setListenersForEqualizer() = binding.apply {
        enableEqualizer.setOnCheckedChangeListener { _, isChecked ->
            equalizer.setEnable(isChecked)
            binding.selectPresets.isEnabled = isChecked
            viewModel.setEqualizerData(isChecked, equalizer.getBandsLevels())
        }
        equalizer.setBandListener(object : EqualizerView.OnChangeListener {
            override fun onBandLevelChanging(bandId: Int, bandLevel: Int) {
                viewModel.setEqualizerBandLevel(bandId, bandLevel)
                binding.selectPresets.text = getString(R.string.label_custom)
            }

            override fun onBandLevelsChanged(bandsLevel: Map<Int, Int>) {
                viewModel.setEqualizerData(enableEqualizer.isChecked, bandsLevel)
                viewModel.setPreset(EqualizerData.CUSTOM_PRESET_ID)
            }
        })
    }

    private fun openDialogToSelectPreset() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.label_presets)
            val presets = viewModel.audioEffectManager.getPresets()
            setItems(presets) { _, which ->
                binding.apply {
                    selectPresets.text = presets[which]
                    equalizer.setBandLevels(viewModel.getBandLevelsForPreset(which))
                }
                viewModel.setPreset(which)
            }
        }.create().show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}