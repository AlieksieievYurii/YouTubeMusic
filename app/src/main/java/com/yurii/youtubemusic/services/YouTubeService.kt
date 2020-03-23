package com.yurii.youtubemusic.services

import android.os.AsyncTask
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import java.lang.Exception
import java.lang.IllegalStateException

class YouTubeService {
    class PlayLists private constructor(
        private val googleAccountCredential: GoogleAccountCredential?,
        private val onResult: ((List<Playlist>) -> Unit)?,
        private val onError: ((error: Exception) -> Unit)?
    ) {

        fun execute() {
            googleAccountCredential?.let {
                PlayListsTask(it, onResult, onError).execute()
            } ?: throw IllegalStateException("Please set google account credential")
        }

        private class PlayListsTask(
            googleAccountCredential: GoogleAccountCredential,
            val onResult: ((List<Playlist>) -> Unit)?,
            val onError: ((error: Exception) -> Unit)?
        ) : AsyncTask<Void, Void, List<Playlist>>() {

            private val service: YouTube
            private var lastError: Exception? = null

            init {
                val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
                val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
                service = YouTube.Builder(transport, jsonFactory, googleAccountCredential)
                    .setApplicationName("YouTube Data API PlayLists")
                    .build()
            }

            override fun doInBackground(vararg params: Void?): List<Playlist>? {
                return try {
                    getPlayLists()
                } catch (error: Exception) {
                    lastError = error
                    cancel(true)
                    null
                }
            }

            override fun onPostExecute(result: List<Playlist>?) {
                result?.let {
                    onResult?.invoke(it)
                }
            }

            private fun getPlayLists(): List<Playlist> =
                service.playlists().list("snippet,contentDetails")
                    .setMine(true)
                    .setMaxResults(50)
                    .execute().items
            //TODO Implement mechanism which obtains all items if results size is more than 50

            override fun onCancelled() {
                if (lastError != null && onError != null)
                    onError.invoke(lastError!!)
            }

        }

        data class Builder(
            var googleAccountCredential: GoogleAccountCredential? = null,
            var onResult: ((List<Playlist>) -> Unit)? = null,
            var onError: ((error: Exception) -> Unit)? = null
        ) {

            fun googleAccountCredential(googleAccountCredential: GoogleAccountCredential) = apply {
                this.googleAccountCredential = googleAccountCredential
            }

            fun onResult(onResult: ((List<Playlist>) -> Unit)) = apply {
                this.onResult = onResult
            }

            fun onError(onError: ((error: Exception) -> Unit)?) = apply {
                this.onError = onError
            }

            fun build() = PlayLists(googleAccountCredential, onResult, onError)
        }
    }

    class PlayListVideos private constructor(
        private val googleAccountCredential: GoogleAccountCredential?,
        private val playListId: String?,
        private val onResult: ((List<PlaylistItem>) -> Unit)?,
        private val onError: ((error: Exception) -> Unit)?
    ) {

        fun execute() {
            googleAccountCredential?.let { credential: GoogleAccountCredential ->
                playListId?.let { PlayListVideosTask(credential, onResult, onError).execute(it) }
                    ?: throw  IllegalStateException("Please set playList id")
            } ?: throw IllegalStateException("Please set google account credential")
        }

        private class PlayListVideosTask(
            googleAccountCredential: GoogleAccountCredential?,
            val onResult: ((List<PlaylistItem>) -> Unit)?,
            val onError: ((error: Exception) -> Unit)?
        ) : AsyncTask<String, Void, List<PlaylistItem>>() {

            private val service: YouTube
            private var lastError: Exception? = null

            init {
                val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
                val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
                service = YouTube.Builder(transport, jsonFactory, googleAccountCredential)
                    .setApplicationName("YouTube Data API PlayLists")
                    .build()
            }

            override fun doInBackground(vararg params: String): List<PlaylistItem>? {
                return try {
                    getPlayListVideos(params.first())
                } catch (error: Exception) {
                    lastError = error
                    cancel(true)
                    null
                }
            }

            private fun getPlayListVideos(playListId: String): List<PlaylistItem> =
                //TODO Implement pagination
                service.playlistItems().list("snippet")
                    .setPlaylistId(playListId)
                    .setMaxResults(50)
                    .execute().items


            override fun onPostExecute(result: List<PlaylistItem>?) {
                result?.let {
                    onResult?.invoke(it)
                }
            }

            override fun onCancelled() {
                if (lastError != null && onError != null)
                    onError.invoke(lastError!!)
            }
        }

        data class Builder(
            var googleAccountCredential: GoogleAccountCredential? = null,
            var playListId: String? = null,
            var onResult: ((List<PlaylistItem>) -> Unit)? = null,
            var onError: ((error: Exception) -> Unit)? = null
        ) {
            fun googleAccountCredential(googleAccountCredential: GoogleAccountCredential) = apply {
                this.googleAccountCredential = googleAccountCredential
            }

            fun playListId(playListId: String) = apply {
                this.playListId = playListId
            }

            fun onResult(onResult: ((List<PlaylistItem>) -> Unit)) = apply {
                this.onResult = onResult
            }

            fun onError(onError: ((error: Exception) -> Unit)?) = apply {
                this.onError = onError
            }

            fun build() = PlayListVideos(googleAccountCredential, playListId, onResult, onError)
        }
    }
}