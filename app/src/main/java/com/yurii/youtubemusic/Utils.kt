package com.yurii.youtubemusic

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.model.Playlist

const val SHARED_PREFERENCES_SELECTED_PLAY_LIST: String = "com.yurii.youtubemusic.shared.preferences.selected.play.list"

class Preferences private constructor() {
    companion object {
        fun setSelectedPlayList(activity: Activity, playList: Playlist) {
            val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, playList.toString())
                apply()
            }
        }

        fun getSelectedPlayList(activity: Activity): Playlist? {
            val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
            val jsonRepresentation: String? = sharedPreferences.getString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, null)
            jsonRepresentation?.let {
                val jsonFactory: com.google.api.client.json.JsonFactory = JacksonFactory.getDefaultInstance()
                return jsonFactory.fromString(it, Playlist::class.java)
            } ?: return null
        }
    }
}

class ErrorSnackBar private constructor() {
    companion object {
        fun show(view: View, details: String) {
            val snackBar = Snackbar.make(view, details, Snackbar.LENGTH_LONG)
            snackBar.view.setBackgroundColor(Color.RED)
            snackBar.show()
        }
    }
}

