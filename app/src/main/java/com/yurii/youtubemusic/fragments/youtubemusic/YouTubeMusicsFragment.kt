package com.yurii.youtubemusic.fragments.youtubemusic

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.util.Utils
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.yurii.youtubemusic.ErrorSnackBar
import com.yurii.youtubemusic.Preferences
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.YouTubeService
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListsDialogFragment
import com.yurii.youtubemusic.fragments.authorization.AuthorizationFragment
import com.yurii.youtubemusic.videoslist.VideosListAdapter

class YouTubeMusicsFragment(private val mCredential: GoogleAccountCredential) : Fragment() {
    private lateinit var binding: FragmentYouTubeMusicsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        binding.btnSelectPlayList.setOnClickListener {
            selectPlayList()
        }
        return binding.root
    }

    private fun selectPlayList() {
        val playListsDialogFragment = PlayListsDialogFragment(object : PlayListsDialogFragment.OnPlayLists {
            override fun getPlayLists(onResult: (playLists: List<Playlist>) -> Unit) {
                YouTubeService.PlayLists.Builder(mCredential)
                    .onResult(onResult)
                    .onError {
                        ErrorSnackBar.show(binding.root, it.message!!)
                        if (it is UserRecoverableAuthIOException) {
                            startActivityForResult(it.intent, AuthorizationFragment.REQUEST_AUTHORIZATION)
                        } else
                            Toast.makeText(context, it.toString(), Toast.LENGTH_LONG).show()
                    }.build().execute()
            }
        })
        playListsDialogFragment.onSelectPlaylist = {
            if (Preferences.getSelectedPlayList(activity!!).isNullOrEmpty())
                alterSelectionPlayListButton()

            Preferences.setSelectedPlayList(activity!!, it)
            loadListOfVideo(it)
        }

        playListsDialogFragment.showPlayLists(activity!!.supportFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        val playList = Preferences.getSelectedPlayList(activity!!)
        playList?.let {
            loadListOfVideo(it)
        } ?: showOptionToSelectPlayList()
    }

    private fun alterSelectionPlayListButton(): Unit =
        binding.let{
            it.btnSelectPlayListFirst.visibility = View.GONE
            it.layoutSelectionPlaylist.visibility = View.VISIBLE
        }

    private fun showOptionToSelectPlayList() {
        binding.btnSelectPlayListFirst.setOnClickListener { selectPlayList() }

        binding.apply {
            btnSelectPlayListFirst.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            layoutSelectionPlaylist.visibility = View.GONE
        }
    }

    private fun loadListOfVideo(playList: Playlist) {
        binding.progressBar.visibility = View.VISIBLE
        binding.videos.visibility = View.GONE
        YouTubeService.PlayListVideos.Builder(mCredential)
            .playListId(playList.id)
            .onResult {
                binding.videos.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    adapter = VideosListAdapter(it)
                }
                binding.progressBar.visibility = View.GONE
                binding.videos.visibility = View.VISIBLE
            }
            .onError {
                ErrorSnackBar.show(binding.root, it.message!!)
            }.build().execute()
    }
}
