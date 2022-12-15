package com.yurii.youtubemusic.models

import android.media.audiofx.Equalizer


data class TwisterData(
    val isEnabled: Boolean,
    val value: Int
) {
    companion object {
        fun create() = TwisterData(false, 0)
    }
}

data class EqualizerData(
    val isEnabled: Boolean,
    var lowestBandLevel: Short,
    var highestBandLevel: Short,
    var listOfCenterFreq: ArrayList<Int>,
    var bandsLevels: Map<Int, Int>,
    var currentPreset: Int
) {
    companion object {
        const val CUSTOM_PRESET_ID = -1

        fun create(): EqualizerData {
            val globalEqualizer = Equalizer(0, 0)
            val listOfCenterFreq = ArrayList<Int>()
            (0 until globalEqualizer.numberOfBands).map { globalEqualizer.getCenterFreq(it.toShort()) }.mapTo(listOfCenterFreq) { it / 1000 }
            val bandsLevels = HashMap<Int, Int>()
            val middle = globalEqualizer.bandLevelRange.last() + globalEqualizer.bandLevelRange.first()
            (0 until globalEqualizer.numberOfBands).forEach { bandsLevels[it] = middle }

            return EqualizerData(
                false,
                globalEqualizer.bandLevelRange.first(),
                globalEqualizer.bandLevelRange.last(),
                listOfCenterFreq, bandsLevels,
                CUSTOM_PRESET_ID
            )
        }
    }
}

