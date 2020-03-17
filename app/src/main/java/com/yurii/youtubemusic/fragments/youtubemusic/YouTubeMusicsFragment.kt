package com.yurii.youtubemusic.fragments

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.yurii.youtubemusic.R
import java.lang.Exception
import com.yurii.youtubemusic.MainActivity
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import android.R



class YouTubeMusicsFragment(private val mCredential: GoogleAccountCredential) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        return inflater.inflate(R.layout.fragment_you_tube_musics, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val task = MakeRequestTask(mCredential)
        task.execute()
    }

    private inner class MakeRequestTask(private val mCredential: GoogleAccountCredential) :
        AsyncTask<Void, Void, Void>() {

        private var mService: YouTube

        init {
            val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
            mService = YouTube.Builder(
                transport, jsonFactory, mCredential
            )
                .setApplicationName("YouTube Data API")
                .build()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                getDataFromApi()
            } catch (error: Exception) {

            }
        }

        private fun getDataFromApi() {
            val result: YouTube.Playlists.List =
                mService.playlists().list("snippet,contentDetails")
            result.mine = true
            result.execute()
            Log.e("TEST", result.toString())
        }

    }
}
