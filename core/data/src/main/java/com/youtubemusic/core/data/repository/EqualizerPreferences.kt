package com.youtubemusic.core.data.repository

import android.content.Context
import com.google.gson.Gson
import com.youtubemusic.core.model.EqualizerData
import com.youtubemusic.core.model.TwisterData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EqualizerPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    fun getBassBoostData(): TwisterData {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_BASS_BOOST_DATA, null) ?: return TwisterData.create()
        return Gson().fromJson(audioEffectsDataJson, TwisterData::class.java)
    }

    fun setBassBoostData(bassBoostData: TwisterData) {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_BASS_BOOST_DATA, Gson().toJson(bassBoostData))
            apply()
        }
    }

    fun getVirtualizerData(): TwisterData {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_VIRTUALIZER_DATA, null) ?: return TwisterData.create()
        return Gson().fromJson(audioEffectsDataJson, TwisterData::class.java)
    }

    fun setVirtualizerData(virtualizerData: TwisterData) {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_VIRTUALIZER_DATA, Gson().toJson(virtualizerData))
            apply()
        }
    }

    fun getEqualizerData(): EqualizerData {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        val audioEffectsDataJson = sharedPreferences.getString(SH_KEY_EQUALIZER_DATA, null) ?: return EqualizerData.create()
        return Gson().fromJson(audioEffectsDataJson, EqualizerData::class.java)
    }

    fun setEqualizerData(equalizerData: EqualizerData) {
        val sharedPreferences = context.getSharedPreferences(EQUALIZER_PREFERENCES, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_EQUALIZER_DATA, Gson().toJson(equalizerData))
            apply()
        }
    }

    companion object {
        private const val EQUALIZER_PREFERENCES = "com.yurii.youtubemusic.shared.preferences.equalizer"
        private const val SH_KEY_BASS_BOOST_DATA: String = "com.yurii.youtubemusic.bass.boost"
        private const val SH_KEY_VIRTUALIZER_DATA: String = "com.yurii.youtubemusic.virtualizer"
        private const val SH_KEY_EQUALIZER_DATA: String = "com.yurii.youtubemusic.bass.equalizer"
    }
}