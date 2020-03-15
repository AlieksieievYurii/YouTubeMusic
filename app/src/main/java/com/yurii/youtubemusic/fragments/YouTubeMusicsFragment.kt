package com.yurii.youtubemusic.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.yurii.youtubemusic.R

class YouTubeMusicsFragment(private val mCredential: GoogleAccountCredential) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?): View? {

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        return inflater.inflate(R.layout.fragment_you_tube_musics, container, false)
    }
}
