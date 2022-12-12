package com.yurii.youtubemusic.screens.equalizer

import androidx.lifecycle.*
import java.lang.IllegalStateException

class EqualizerViewModel : ViewModel() {
    //TODO add handling equalizer
    @Suppress("UNCHECKED_CAST")
    class Factory() : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EqualizerViewModel::class.java))
                return EqualizerViewModel() as T
            throw IllegalStateException("Given the model class is not assignable from EqualizerViewModel class")
        }
    }
}

