package com.yurii.youtubemusic.utilities

import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.model.Playlist
import com.google.gson.Gson
import com.yurii.youtubemusic.models.VideoItem
import org.threeten.bp.Duration
import java.io.File
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

const val DEFAULT_SHARED_PREFERENCES_FILE: String = "com.yurii.youtubemusic.shared.preferences"
const val SHARED_PREFERENCES_SELECTED_PLAY_LIST: String = "com.yurii.youtubemusic.shared.preferences.selected.play.list"
const val SH_KEY_MUSICS_CATEGORIES: String = "com.yurii.youtubemusic.shared.preferences.music.categories"

object Preferences {
    class ValuesNotFound(key: String) : Exception("Value is not found for $key")

    @Throws(ValuesNotFound::class)
    fun getMusicCategories(context: Context): Array<String> {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val categoriesJson: String = sharedPreferences.getString(SH_KEY_MUSICS_CATEGORIES, null) ?: throw ValuesNotFound(SH_KEY_MUSICS_CATEGORIES)
        return Gson().fromJson(categoriesJson, Array<String>::class.java)
    }

    fun setCategories(context: Context, categories: List<String>) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SH_KEY_MUSICS_CATEGORIES, Gson().toJson(categories))
            apply()
        }
    }

    fun setSelectedPlayList(context: Context, playList: Playlist?) {
        val sharedPreferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(SHARED_PREFERENCES_SELECTED_PLAY_LIST, playList?.toString())
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

class ErrorSnackBar private constructor() {
    companion object {
        fun show(view: View, details: String) {
            val snackBar = Snackbar.make(view, details, Snackbar.LENGTH_LONG)
            snackBar.view.setBackgroundColor(Color.RED)
            snackBar.show()
        }
    }
}

object DataStorage {
    fun getMusicStorage(context: Context): File = File(context.filesDir, "Musics")
    private fun getThumbnailsStorage(context: Context): File = File(context.filesDir, "Thumbnails")
    private fun getMusicMetadataStorage(context: Context): File = File(context.filesDir, "Metadata")

    fun getMusic(context: Context, musicId: String): File = File(getMusicStorage(context), "$musicId.mp3")
    fun getThumbnail(context: Context, musicId: String): File = File(getThumbnailsStorage(context), "$musicId.jpeg")
    fun getMetadata(context: Context, musicId: String): File = File(getMusicMetadataStorage(context), "$musicId.json")

    fun getAllMusicFiles(context: Context): List<File> = getMusicStorage(context).walk().filter { it.extension == "mp3" }.toList()
}

fun parseDurationToHumanView(text: String): String {
    val millis = Duration.parse(text).toMillis()
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)

    val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()

    return if (hours > 0) "$hours:$minutes:$secondsString" else "$minutes:$secondsString"
}

fun bigIntegerToShortCutSuffix(value: BigInteger): String {
    return longToShortCutSuffix(value.toLong())
}

fun longToShortCutSuffix(value: Long): String {
    val suffixes: TreeMap<Long, Char> = TreeMap()
    suffixes[1_000L] = 'K'
    suffixes[1_000_000L] = 'M'
    suffixes[1_000_000_000L] = 'B'
    suffixes[1_000_000_000_000L] = 'T'
    suffixes[1_000_000_000_000_000L] = 'P'

    if (value == java.lang.Long.MIN_VALUE) return longToShortCutSuffix(java.lang.Long.MIN_VALUE + 1)
    if (value < 0) return "-${longToShortCutSuffix(-value)}"
    if (value < 1000) return value.toString()

    val e = suffixes.floorEntry(value)
    val divideBy: Long = e!!.key
    val suffix: Char = e.value

    val truncated: Long = value / (divideBy / 10)
    val hasDecimal: Boolean = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
}

fun calculateLikeBarValue(likeCount: BigInteger, disLikeCount: BigInteger): Int {
    val sum = likeCount.plus(disLikeCount)
    return if (sum.compareTo(BigInteger.ZERO) == 0) 50 else likeCount.multiply(BigInteger("100")).divide(sum).toInt()
}



