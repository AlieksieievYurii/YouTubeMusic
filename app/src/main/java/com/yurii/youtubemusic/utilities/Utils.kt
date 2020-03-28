package com.yurii.youtubemusic.utilities

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import com.google.api.services.youtube.model.Playlist
import org.threeten.bp.Duration
import java.io.File
import java.util.concurrent.TimeUnit

const val DEFAULT_SHARED_PREFERENCES_FILE: String = "com.yurii.youtubemusic.shared.preferences"
const val SHARED_PREFERENCES_SELECTED_PLAY_LIST: String = "com.yurii.youtubemusic.shared.preferences.selected.play.list"
const val PREF_ACCOUNT_NAME = "accountName"

class Preferences private constructor() {
    companion object {
        fun setSelectedPlayList(context: Context, playList: Playlist) {
            val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, playList.toString())
                apply()
            }
        }

        fun getSelectedPlayList(context: Context): Playlist? {
            val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
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

class DataStorage private constructor() {
    companion object {
        fun getMusicStorage(context: Context): File = File(context.filesDir, "Musics")
    }
}

class Authorization private constructor() {
    companion object {
        private val scopes = listOf(YouTubeScopes.YOUTUBE)
        fun getGoogleCredentials(context: Context): GoogleAccountCredential? {
            val accountName: String? = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null)

            accountName?.let {
                return GoogleAccountCredential.usingOAuth2(context, scopes).setBackOff(ExponentialBackOff()).also {
                    it.selectedAccountName = accountName
                }
            }
            return null
        }

        fun getGoogleAccount(context: Context): String? {
            val preferences: SharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
            return preferences.getString(PREF_ACCOUNT_NAME, null)
        }

        fun setGoogleAccount(context: Context, accountName: String) {
            val preferences: SharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
            with(preferences.edit()) {
                putString(PREF_ACCOUNT_NAME, accountName)
                commit()
            }
        }
    }
}

class DateTime private constructor() {
    companion object {
        fun parseToHumanView(text: String): String {
            val millis = Duration.parse(text).toMillis()
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)

            val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()

            return if (hours > 0) "$hours:$minutes:$secondsString" else "$minutes:$secondsString"
        }
    }
}


