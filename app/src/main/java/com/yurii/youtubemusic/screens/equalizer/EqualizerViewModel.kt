package com.yurii.youtubemusic.screens.equalizer

import androidx.lifecycle.*
import com.yurii.youtubemusic.models.TwisterData
import com.yurii.youtubemusic.services.media.AudioEffectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.IllegalStateException
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(val audioEffectManager: AudioEffectManager) : ViewModel() {

    fun getBassBoostData() = audioEffectManager.getBassBoostData()

    fun getVirtualizerData() = audioEffectManager.getVirtualizerData()

    fun getEqualizerData() = audioEffectManager.getEqualizerData()

    fun setEqualizerBandLevel(bandId: Int, bandLevel: Int) = audioEffectManager.setEqualizerBandLevel(bandId, bandLevel)

    fun setBassBoost(isEnabled: Boolean, value: Int) {
        val data = TwisterData(isEnabled, value)
        audioEffectManager.setBassBoostState(data)
    }

    fun setVirtualizer(isEnabled: Boolean, value: Int) {
        val data = TwisterData(isEnabled, value)
        audioEffectManager.setVirtualizerData(data)
    }

    fun setEqualizerData(isEnabled: Boolean, bandsLevels: Map<Int, Int>) {
        val newData = audioEffectManager.getEqualizerData().copy(isEnabled = isEnabled, bandsLevels = bandsLevels)
        audioEffectManager.setEqualizerData(newData)
    }

    fun setPreset(presetId: Int) {
        audioEffectManager.setPreset(presetId)
    }

    fun getPresetName(presetId: Int): String = audioEffectManager.getPresetName(presetId)

    fun getBandLevelsForPreset(presetId: Int) = audioEffectManager.getBandLevelsForPreset(presetId)
}

