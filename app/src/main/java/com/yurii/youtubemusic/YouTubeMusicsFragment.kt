package com.yurii.youtubemusic

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListsDialogFragment
import com.yurii.youtubemusic.services.YouTubeService
import com.yurii.youtubemusic.utilities.Authorization
import com.yurii.youtubemusic.utilities.ErrorSnackBar
import com.yurii.youtubemusic.utilities.Preferences
import com.yurii.youtubemusic.utilities.VideoItemsHandler
import java.lang.IllegalStateException

class YouTubeMusicsFragment : Fragment() {
    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videoItemsHandler: VideoItemsHandler
    private lateinit var mCredential: GoogleAccountCredential

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        binding.btnSelectPlayList.setOnClickListener {
            selectPlayList()
        }
        videoItemsHandler = VideoItemsHandler(binding.videos)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Authorization.getGoogleCredentials(context)?.let {
            mCredential = it
        } ?: throw IllegalStateException("Cannot get Google account credentials")
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

            Preferences.setSelectedPlayList(context!!, it)
            loadListOfVideo(it)
        }

        playListsDialogFragment.showPlayLists(activity!!.supportFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        val playList = Preferences.getSelectedPlayList(context!!)
        videoItemsHandler.onStart()
        playList?.let {
            loadListOfVideo(it)
        } ?: showOptionToSelectPlayListFirstTime()
    }

    override fun onStop() {
        super.onStop()
        videoItemsHandler.onStop()
    }

    private fun alterSelectionPlayListButton(): Unit =
        binding.let{
            it.btnSelectPlayListFirst.visibility = View.GONE
            it.layoutSelectionPlaylist.visibility = View.VISIBLE
        }

    private fun showOptionToSelectPlayListFirstTime() {
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
                videoItemsHandler.setVideoItems(it)
                binding.progressBar.visibility = View.GONE
                binding.videos.visibility = View.VISIBLE
            }
            .onError {
                ErrorSnackBar.show(binding.root, it.message!!)
            }.build().execute()
    }
}
