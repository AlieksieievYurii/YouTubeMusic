package com.yurii.youtubemusic.utilities

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.annotation.IntRange
import com.yurii.youtubemusic.models.AudioEffectsData

class AudioEffectManager(private  val preferences: IPreferences) {
    val data: AudioEffectsData =  preferences.getAudioEffectsData()
    private var sessionId: Int? = null

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null


    fun applyLastChanges(sessionId: Int) {
        if (this.sessionId == null || this.sessionId != sessionId) {
            this.sessionId = sessionId
            bassBoost = BassBoost(0, sessionId)
            virtualizer = Virtualizer(0, sessionId)
            equalizer = Equalizer(0, sessionId)
            applyAudioAffects()
        }
    }

    fun setEnableEqualizer(enable: Boolean) {
        data.enableEqualizer = enable
        equalizer?.enabled = enable
    }

    fun setEnableBassBoost(enable: Boolean) {
        data.enableBassBoost = enable
        bassBoost?.enabled = enable
    }

    fun setEnableVirtualizer(enable: Boolean) {
        data.enableVirtualizer = enable
        virtualizer?.enabled = enable
    }

    private fun applyAudioAffects() {
        bassBoost?.apply {
            enabled = data.enableBassBoost
            setStrength(convertToAudioEffectRange(data.bassBoost))
        }
        virtualizer?.apply {
            enabled = data.enableVirtualizer
            setStrength(convertToAudioEffectRange(data.virtualizer))
        }
        equalizer?.apply {
            for ((bandId, level) in data.bandsLevels) {
                setBandLevel(bandId, level)
            }
            enabled = data.enableEqualizer
        }

    }

    fun getPresets(): Array<String> {
        val eq = equalizer ?: getDefaultEqualizer()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }.toTypedArray()
    }

    fun setBandLevel(band: Int, level: Int) {
        equalizer?.setBandLevel(band.toShort(), level.toShort())
        data.bandsLevels[band] = level
    }

    fun setPreset(id: Int) {
        equalizer?.usePreset(id.toShort())
    }

    fun getBandLevelsForPreset(presetId: Int): HashMap<Int, Int> {
        val eq = equalizer ?: getDefaultEqualizer()
        eq.usePreset(presetId.toShort())
        val res = HashMap<Int, Int>()
        (0 until eq.numberOfBands).forEach {
            res[it] = eq.getBandLevel(it.toShort()).toInt()
        }
        return res
    }

    private fun getDefaultEqualizer(): Equalizer = Equalizer(0, 0).apply { enabled = false }

    fun setBassBoost(@IntRange(from = 0, to = 1000) strength: Int) {
        data.bassBoost = strength
        bassBoost?.setStrength(convertToAudioEffectRange(strength))
    }

    fun setVirtualizer(@IntRange(from = 0, to = 1000) strength: Int) {
        data.virtualizer = strength
        virtualizer?.setStrength(convertToAudioEffectRange(strength))
    }

    private fun convertToAudioEffectRange(value: Int): Short = (value * 10).toShort()
    fun saveChanges() = preferences.setAudioEffectsData(data)
}