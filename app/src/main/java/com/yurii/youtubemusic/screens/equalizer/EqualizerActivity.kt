package com.yurii.youtubemusic.screens.equalizer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityEqualizerBinding
import com.yurii.youtubemusic.models.EqualizerData
import com.yurii.youtubemusic.ui.EqualizerView
import com.yurii.youtubemusic.utilities.Injector
import kotlinx.coroutines.launch


class EqualizerActivity : AppCompatActivity() {
    private val viewModel: EqualizerViewModel by viewModels { Injector.provideEqualizerViewModel(application) }
    private val binding: ActivityEqualizerBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.label_audio_effects)
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { syncBassBoostState() }
                launch { syncVirtualizerState() }
                launch { syncEqualizerData() }
            }
        }

        setListenersForBoostBass()
        setListenersForVirtualizer()
        setListenersForEqualizer()
    }

    private suspend fun syncEqualizerData() {
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
    }

    private suspend fun syncBassBoostState() {
        viewModel.getBassBoostData().let {
            binding.apply {
                bassBoost.setEnable(it.isEnabled)
                bassBoost.value = it.value
                enableBassBoost.isChecked = it.isEnabled
            }
        }
    }

    private suspend fun syncVirtualizerState() {
        viewModel.getVirtualizerData().let {
            binding.apply {
                virtualizer.setEnable(it.isEnabled)
                virtualizer.value = it.value
                enableVirtualizer.isChecked = it.isEnabled
            }
        }
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
                viewModel.setEqualizerData(enableEqualizer.isChecked, equalizer.getBandsLevels())
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}