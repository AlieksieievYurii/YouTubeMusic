package com.yurii.youtubemusic.services.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.yurii.youtubemusic.models.EqualizerData
import com.yurii.youtubemusic.models.TwisterData
import com.yurii.youtubemusic.source.EqualizerPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectManager @Inject constructor(private val preferences: EqualizerPreferences) {
    private var currentSessionId: Int? = null

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null

    /**
     * Sets current session by [sessionId].
     */
    fun setSession(sessionId: Int) {
        if (currentSessionId != sessionId) {
            currentSessionId = sessionId
            bassBoost = BassBoost(0, sessionId)
            virtualizer = Virtualizer(0, sessionId)
            equalizer = Equalizer(0, sessionId)
        }
    }

    /**
     * If the session is assigned by [setSession], then this call applies saved audio effects
     */
    fun applyCurrentAffects() {
        currentSessionId?.let {
            applyBassBoost(getBassBoostData())
            applyVirtualizer(getVirtualizerData())
            applyEqualizer(getEqualizerData())
        }
    }

    /**
     * Returns all available presets names
     */
    fun getPresets(): Array<String> {
        val eq = equalizer ?: getDefaultEqualizer()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }.toTypedArray()
    }

    /**
     * Sets given [presetId]. If the value is not [EqualizerData.CUSTOM_PRESET_ID], then system preset bands levels are applied
     */
    fun setPreset(presetId: Int) {
        val data = getEqualizerData()
        if (presetId != EqualizerData.CUSTOM_PRESET_ID) {
            equalizer?.usePreset(presetId.toShort())
            setEqualizerData(data.copy(currentPreset = presetId, bandsLevels = getBandLevelsForPreset(presetId)))
        } else
            setEqualizerData(data.copy(currentPreset = presetId))
    }

    /**
     * Returns the list of system defined preset band levels
     */
    fun getBandLevelsForPreset(presetId: Int): HashMap<Int, Int> {
        val eq = equalizer ?: getDefaultEqualizer()
        eq.usePreset(presetId.toShort())
        val res = HashMap<Int, Int>()
        (0 until eq.numberOfBands).forEach {
            res[it] = eq.getBandLevel(it.toShort()).toInt()
        }
        return res
    }

    fun getPresetName(presetId: Int): String {
        val eq = equalizer ?: getDefaultEqualizer()
        return eq.getPresetName(presetId.toShort())
    }

    fun getBassBoostData(): TwisterData = preferences.getBassBoostData()

    fun getVirtualizerData(): TwisterData = preferences.getVirtualizerData()

    fun getEqualizerData(): EqualizerData = preferences.getEqualizerData()

    fun setBassBoostState(data: TwisterData) {
        preferences.setBassBoostData(data)
        applyBassBoost(data)
    }

    fun setVirtualizerData(data: TwisterData) {
         preferences.setVirtualizerData(data)
        applyVirtualizer(data)
    }

    fun setEqualizerData(data: EqualizerData) {
       preferences.setEqualizerData(data)
        applyEqualizer(data)
    }

    /**
     * Sets Equalizer band level BUT does not save it to the [preferences]
     */
    fun setEqualizerBandLevel(bandId: Int, bandLevel: Int) {
        equalizer?.apply {
            setBandLevel(bandId.toShort(), bandLevel.toShort())
        }
    }

    private fun applyBassBoost(bassBoostData: TwisterData) {
        bassBoost?.apply {
            enabled = bassBoostData.isEnabled
            setStrength(convertToAudioEffectRange(bassBoostData.value))
        }
    }

    private fun applyVirtualizer(bassBoostData: TwisterData) {
        virtualizer?.apply {
            enabled = bassBoostData.isEnabled
            setStrength(convertToAudioEffectRange(bassBoostData.value))
        }
    }

    private fun applyEqualizer(equalizerData: EqualizerData) {
        equalizer?.apply {
            for ((bandId, level) in equalizerData.bandsLevels)
                setBandLevel(bandId.toShort(), level.toShort())
            enabled = equalizerData.isEnabled
        }
    }

    private fun getDefaultEqualizer(): Equalizer = Equalizer(0, 0).apply { enabled = false }

    private fun convertToAudioEffectRange(value: Int): Short = (value * 10).toShort()
}