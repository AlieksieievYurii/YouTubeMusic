package com.yurii.youtubemusic.utilities

import android.content.Context
import androidx.annotation.VisibleForTesting

object ServiceLocator {
    var preferences: IPreferences? = null
        @VisibleForTesting set

    fun providePreferences(context: Context): IPreferences {
        synchronized(this) {
            return preferences ?: createPreferences(context)
        }
    }

    private fun createPreferences(context: Context): IPreferences {
        return Preferences(context).also {
            preferences = it
        }
    }
}