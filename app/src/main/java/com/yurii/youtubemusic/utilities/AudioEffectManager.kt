package com.yurii.youtubemusic.utilities

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.annotation.IntRange
import com.yurii.youtubemusic.models.AudioEffectsData

class AudioEffectManager(private val context: Context) {
    val data: AudioEffectsData = Preferences.getAudioEffectsData(context)
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

    fun setBandLevel(band: Int, level: Int) {
        equalizer?.setBandLevel(band.toShort(), level.toShort())
        data.bandsLevels[band] = level
    }

    fun setBassBoost(@IntRange(from = 0, to = 1000) strength: Int) {
        data.bassBoost = strength
        bassBoost?.setStrength(convertToAudioEffectRange(strength))
    }

    fun setVirtualizer(@IntRange(from = 0, to = 1000) strength: Int) {
        data.virtualizer = strength
        virtualizer?.setStrength(convertToAudioEffectRange(strength))
    }

    private fun convertToAudioEffectRange(value: Int): Short = (value * 10).toShort()
    fun saveChanges() = Preferences.setAudioEffectsData(context, data)
}