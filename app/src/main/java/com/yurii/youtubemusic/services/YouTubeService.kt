package com.yurii.youtubemusic.services

import android.os.AsyncTask
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import java.lang.Exception

typealias ResultCallBack<T> = (result: T, nextPageToken: String?) -> Unit
typealias ErrorCallBack = (error: Exception) -> Unit

class YouTubeService private constructor() {
    class MyPlayLists(googleAccountCredential: GoogleAccountCredential) : AsyncTask<String, Void, PlaylistListResponse>()  {
        private var onResult: ResultCallBack<List<Playlist>>? = null
        private var onError: ErrorCallBack? = null
        private val service: YouTube

        init {
            val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
            service = YouTube.Builder(transport, jsonFactory, googleAccountCredential).build()
        }

        fun setOnResult(onResult: ResultCallBack<List<Playlist>>) = apply { this.onResult = onResult }
        fun setOnError(onError: ErrorCallBack) = apply { this.onError = onError }

        override fun doInBackground(vararg params: String): PlaylistListResponse? {
            return try {
                service.playlists().list("snippet,contentDetails")
                    .setMine(true)
                    .setPageToken(params[0]) // First parameter must be a page token
                    .setMaxResults(15)
                    .execute()
            } catch (error: Exception) {
                onError?.invoke(error)
                cancel(true)
                null
            }
        }

        override fun onPostExecute(result: PlaylistListResponse?) {
            result?.let { onResult?.invoke(it.items, it.nextPageToken) }
        }

        fun execute(pageToken: String? = null) {
            super.execute(pageToken)
        }
    }

    class PlayListItems(googleAccountCredential: GoogleAccountCredential) : AsyncTask<String, Void, PlaylistItemListResponse?>() {
        private var onResult: ResultCallBack<List<PlaylistItem>>? = null
        private var onError: ErrorCallBack? = null
        private val service: YouTube

        init {
            val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
            service = YouTube.Builder(transport, jsonFactory, googleAccountCredential).build()
        }

        fun setOnResult(onResult: ResultCallBack<List<PlaylistItem>>) = apply { this.onResult = onResult }
        fun setOnError(onError: ErrorCallBack) = apply { this.onError = onError }

        fun execute(playListId: String, pageToken: String? = null) {
          super.execute(playListId, pageToken)
        }

        override fun doInBackground(vararg params: String): PlaylistItemListResponse? {
            return try {
                service.playlistItems().list("snippet")
                    .setPlaylistId(params[0]) // First parameter must be a playlist id
                    .setMaxResults(10)
                    .setPageToken(params[1]) // Second parameter must be a page token
                    .execute()
            } catch (error: Exception) {
                onError?.invoke(error)
                cancel(true)
                null
            }
        }

        override fun onPostExecute(result: PlaylistItemListResponse?) {
            result?.let { onResult?.invoke(it.items, it.nextPageToken) }
        }

    }

    class VideoDetails(googleAccountCredential: GoogleAccountCredential) : AsyncTask<String, Void, VideoListResponse?>() {
        private var onResult: ResultCallBack<List<Video>>? = null
        private var onError: ErrorCallBack? = null
        private val service: YouTube

        init {
            val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
            service = YouTube.Builder(transport, jsonFactory, googleAccountCredential).build()
        }

        fun setOnResult(onResult: ResultCallBack<List<Video>>) = apply { this.onResult = onResult }
        fun setOnError(onError: ErrorCallBack) = apply { this.onError = onError }

        override fun doInBackground(vararg params: String?): VideoListResponse? {
            return try {
                service.videos().list("snippet,statistics,contentDetails")
                    .setId(params[0]) // Must be video ids
                    .setPageToken(params[1]) // Must be a page token
                    .execute()
            } catch (error: Exception) {
                onError?.invoke(error)
                cancel(true)
                null
            }
        }

        override fun onPostExecute(result: VideoListResponse?) {
            result?.let { onResult?.invoke(it.items, it.nextPageToken) }
        }

        fun execute(ids: List<String>, pageToken: String? = null) {
            super.execute(ids.joinToString(","), pageToken)
        }
    }
}