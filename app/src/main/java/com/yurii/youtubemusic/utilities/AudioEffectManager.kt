package com.yurii.youtubemusic.utilities

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.yurii.youtubemusic.models.EqualizerData
import com.yurii.youtubemusic.models.TwisterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioEffectManager private constructor(private val preferences: Preferences) {
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
    suspend fun applyCurrentAffects() {
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
    suspend fun setPreset(presetId: Int) = withContext(Dispatchers.IO) {
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

    suspend fun getBassBoostData(): TwisterData = withContext(Dispatchers.IO) { preferences.getBassBoostData() }

    suspend fun getVirtualizerData(): TwisterData = withContext(Dispatchers.IO) { preferences.getVirtualizerData() }

    suspend fun getEqualizerData(): EqualizerData = withContext(Dispatchers.IO) { preferences.getEqualizerData() }

    suspend fun setBassBoostState(data: TwisterData) {
        withContext(Dispatchers.IO) { preferences.setBassBoostData(data) }
        applyBassBoost(data)
    }

    suspend fun setVirtualizerData(data: TwisterData) {
        withContext(Dispatchers.IO) { preferences.setVirtualizerData(data) }
        applyVirtualizer(data)
    }

    suspend fun setEqualizerData(data: EqualizerData) {
        withContext(Dispatchers.IO) { preferences.setEqualizerData(data) }
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

    companion object {
        @Volatile
        private var instance: AudioEffectManager? = null

        fun getInstance(preferences: Preferences): AudioEffectManager {
            if (instance == null)
                synchronized(this) {
                    instance = AudioEffectManager(preferences)
                }
            return instance!!
        }
    }
}