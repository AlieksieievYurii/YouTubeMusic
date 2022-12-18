package com.yurii.youtubemusic.utilities

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting

object ServiceLocator {
    var preferences: Preferences? = null
        @VisibleForTesting set

//    fun providePreferences(application: Application)): Preferences {
//        synchronized(this) {
//            return preferences ?: createPreferences(application)
//        }
//    }
//
//    private fun createPreferences(application: Application): Preferences {
//        return Preferences.getInstance(application).also {
//            preferences = it
//        }
//    }
}