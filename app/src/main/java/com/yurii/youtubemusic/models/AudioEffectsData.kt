package com.yurii.youtubemusic.models

import android.media.audiofx.Equalizer


data class AudioEffectsData(
    var enableEqualizer: Boolean,
    var enableBassBoost: Boolean,
    var enableVirtualizer: Boolean,
    var numberOfBands: Short,
    var lowestBandLevel: Short,
    var highestBandLevel: Short,
    var bands: ArrayList<Int>,
    var bandsLevels: HashMap<Int, Int>,
    var bassBoost: Int,
    var virtualizer: Int,
    var currentPreset: String
) {
    companion object {
        fun create(): AudioEffectsData {
            val globalEqualizer = Equalizer(0, 0)
            val listOfCenterFreq = ArrayList<Int>()
            (0 until globalEqualizer.numberOfBands).map { globalEqualizer.getCenterFreq(it.toShort()) }.mapTo(listOfCenterFreq) { it / 1000 }
            val bandsLevels = HashMap<Int, Int>()
            val max = globalEqualizer.bandLevelRange.last() + globalEqualizer.bandLevelRange.first()
            (0 until globalEqualizer.numberOfBands).forEach { bandsLevels[it] = max }
            return AudioEffectsData(
                enableEqualizer = false,
                enableBassBoost = false,
                enableVirtualizer = false,
                numberOfBands = globalEqualizer.numberOfBands,
                lowestBandLevel = globalEqualizer.bandLevelRange.first(),
                highestBandLevel = globalEqualizer.bandLevelRange.last(),
                bands = listOfCenterFreq,
                bandsLevels = bandsLevels,
                bassBoost = 0,
                virtualizer = 0,
                currentPreset = "Custom"
            )
        }
    }
}
