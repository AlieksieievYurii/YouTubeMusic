package com.yurii.youtubemusic

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListsDialogFragment
import com.yurii.youtubemusic.utilities.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.dialogplaylists.PlayListDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideosLoader
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import java.lang.Exception


class YouTubeMusicsFragment : Fragment(), Loader {
    private lateinit var viewModel: YouTubeMusicViewModel

    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videoItemsHandler: VideoItemsHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(
            activity!!,
            YouTubeViewModelFactory(activity!!.application, com.yurii.youtubemusic.services.youtube.YouTubeService())
        ).get(YouTubeMusicViewModel::class.java)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        binding.btnSelectPlayList.setOnClickListener { selectPlayList() }

        videoItemsHandler = VideoItemsHandler(binding.videos, this)

        viewModel.selectedPlaylist.observe(this, Observer { playList ->
            if (playList != null)
                binding.tvPlayListName.text = playList.snippet.title
            else
                showOptionToSelectPlayListFirstTime()
        })

        val currentVideos = viewModel.getCurrentVideos()
        if (currentVideos.isNotEmpty()) {
            videoItemsHandler.setNewVideoItems(currentVideos, viewModel.isLast())
            binding.progressBar.visibility = View.GONE
            binding.videos.visibility = View.VISIBLE
        }

        viewModel.videosLoader = object : VideosLoader {
            override fun onResult(newVideos: List<VideoItem>, isLast: Boolean) {
                if (videoItemsHandler.isVideosEmpty()) {
                    videoItemsHandler.setNewVideoItems(newVideos, isLast)
                    binding.progressBar.visibility = View.GONE
                    binding.videos.visibility = View.VISIBLE
                } else
                    videoItemsHandler.addMoreVideoItems(newVideos, isLast)
            }

            override fun onError(error: Exception) {
                ErrorSnackBar.show(binding.root, error.message!!)
            }

        }

        return binding.root
    }

    private fun selectPlayList() {
        PlayListsDialogFragment().showPlayLists(activity!!.supportFragmentManager, object : PlayListDialogInterface {
            override fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?) {
                viewModel.loadPlayLists(object : YouTubeObserver<PlaylistListResponse> {
                    override fun onResult(result: PlaylistListResponse) = onLoad.invoke(result)

                    override fun onError(error: Exception) {
                        ErrorSnackBar.show(binding.root, error.message!!)
                        if (error is UserRecoverableAuthIOException) {
                            startActivityForResult(error.intent, AuthorizationFragment.REQUEST_AUTHORIZATION)
                        } else
                            Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
                    }
                }, nextPageToken)
            }

            override fun onSelectPlaylist(selectedPlaylist: Playlist) {
                viewModel.setNewPlayList(selectedPlaylist)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        videoItemsHandler.onStart()
    }

    override fun onStop() {
        super.onStop()
        videoItemsHandler.onStop()
    }

    private fun alterSelectionPlayListButton(): Unit =
        binding.let {
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

    override fun onLoadMoreVideoItems() {
        viewModel.loadMoreVideos()
    }

}
