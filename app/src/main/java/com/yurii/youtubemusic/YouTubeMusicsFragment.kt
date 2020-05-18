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
import androidx.recyclerview.widget.RecyclerView
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListsDialogFragment
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.YouTubeService
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.dialogplaylists.PlayListDialogInterface
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import java.lang.Exception


class YouTubeMusicsFragment : Fragment(), Loader {
    private lateinit var viewModel: YouTubeMusicViewModel

    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videoItemsHandler: VideoItemsHandler
    private lateinit var mCredential: GoogleAccountCredential

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(
            activity!!,
            YouTubeViewModelFactory(activity!!.application, com.yurii.youtubemusic.services.youtube.YouTubeService())
        ).get(YouTubeMusicViewModel::class.java)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        binding.btnSelectPlayList.setOnClickListener {
            selectPlayList()
        }
        videoItemsHandler = VideoItemsHandler(binding.videos, this)
        videoItemsHandler.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//               binding.layoutSelectionPlaylist.pivotY =
//                    min(0f, max(-binding.layoutSelectionPlaylist.height.toFloat(), binding.layoutSelectionPlaylist.translationY - dy))
//                if (dy < -binding.layoutSelectionPlaylist.height)
//                    binding.layoutSelectionPlaylist.visibility = View.VISIBLE
//                else if (dy > binding.layoutSelectionPlaylist.height)
//                    binding.layoutSelectionPlaylist.visibility = View.GONE

                //TODO Implement hiding SelectionPlayListLayout on scroll
            }
        })

        viewModel.selectedPlaylist.observe(this, Observer {
            if (it != null) {
                binding.tvPlayListName.text = it.snippet.title
                loadVideoItems(it)
            }else
                showOptionToSelectPlayListFirstTime()
        })

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Authorization.getGoogleCredentials(context)?.let {
            mCredential = it
        } ?: throw IllegalStateException("Cannot get Google account credentials")
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

    override fun onLoadMoreVideoItems(pageToken: String?) {
        val playList = Preferences.getSelectedPlayList(context!!)
        playList?.let {
            loadVideoItems(it, pageToken, loadMore = true)
        } ?: throw IllegalStateException("Cannot load more, because there is not selected a playlist")
    }

    private fun loadVideoItems(playList: Playlist, pageToken: String? = null, loadMore: Boolean = false) {
        if (!loadMore) {
            binding.progressBar.visibility = View.VISIBLE
            binding.videos.visibility = View.GONE
        }

        YouTubeService.PlayListItems(mCredential)
            .setOnResult { onResult, nextPageToken ->
                loadDetails(onResult, nextPageToken, loadMore)
            }
            .setOnError { ErrorSnackBar.show(binding.root, it.message!!) }
            .execute(playList.id, pageToken = pageToken)
    }

    private fun loadDetails(videos: List<PlaylistItem>, nextPageToken: String?, loadMore: Boolean = false) {
        val videoIds: List<String> = videos.map { it.snippet.resourceId.videoId }

        YouTubeService.VideoDetails(mCredential)
            .setOnResult { result, _ ->
                val videoItems = result.map {
                    VideoItem(
                        videoId = it.id,
                        title = it.snippet.title,
                        description = it.snippet.description,
                        duration = it.contentDetails.duration,
                        viewCount = it.statistics.viewCount,
                        likeCount = it.statistics.likeCount,
                        disLikeCount = it.statistics.dislikeCount,
                        authorChannelTitle = it.snippet.channelTitle,
                        thumbnail = it.snippet.thumbnails.default.url
                    )
                }
                if (loadMore)
                    videoItemsHandler.addMoreVideoItems(videoItems, nextPageToken)
                else {
                    videoItemsHandler.setNewVideoItems(videoItems, nextPageToken)
                    binding.progressBar.visibility = View.GONE
                    binding.videos.visibility = View.VISIBLE
                }
            }
            .setOnError {
                ErrorSnackBar.show(binding.root, it.message!!)
            }.execute(videoIds)
    }
}
