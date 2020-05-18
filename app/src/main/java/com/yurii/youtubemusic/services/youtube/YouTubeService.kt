package com.yurii.youtubemusic.services.youtube

import android.os.AsyncTask
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItemListResponse
import com.google.api.services.youtube.model.PlaylistListResponse
import com.google.api.services.youtube.model.VideoListResponse

class YouTubeService : IYouTubeService {
    private val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    private val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    private lateinit var service: YouTube

    override fun setCredentials(credential: GoogleAccountCredential) {
        service = YouTube.Builder(transport, jsonFactory, credential).build()
    }

    override fun loadPlayLists(observer: YouTubeObserver<PlaylistListResponse>, nextPageToken: String?): ICanceler {
        val task = Async<PlaylistListResponse>({
            service.playlists().list("snippet,contentDetails")
                .setMine(true)
                .setPageToken(nextPageToken)
                .setMaxResults(15)
                .execute()
        }, { playlistListResponse ->
            observer.onResult(playlistListResponse)
        })
        task.execute()

        return object : ICanceler {
            override fun cancel() {
                task.cancel(true)
            }

        }
    }

    override fun loadPlayListItems(playlistId: String, observer: YouTubeObserver<PlaylistItemListResponse>, nextPageToken: String?): ICanceler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadVideosDetails(ids: List<String>, observer: YouTubeObserver<VideoListResponse>, nextPageToken: String?): ICanceler {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class Async<T>(private val target: () -> T, private val onResult: (T) -> Unit) : AsyncTask<Void, Void, T>() {
    override fun doInBackground(vararg params: Void?): T {
        return target.invoke()
    }

    override fun onPostExecute(result: T) {
        super.onPostExecute(result)
        onResult.invoke(result)
    }

}