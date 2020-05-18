package com.yurii.youtubemusic

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
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
import com.yurii.youtubemusic.dialogplaylists.PlayListsResultCallBack
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.YouTubeService
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.min
import android.opengl.ETC1.getWidth
import android.view.animation.TranslateAnimation
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory


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
                Log.i("ViewModel", "SetPlayList")
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
        val playListsDialogFragment = PlayListsDialogFragment(object : PlayListsDialogFragment.OnPlayLists {
            override fun getPlayLists(onResult: PlayListsResultCallBack, nextTokenPage: String?) {
                YouTubeService.MyPlayLists(mCredential).setOnResult { result, nextPageToken ->
                    onResult.invoke(result, nextPageToken)
                }.setOnError {
                    ErrorSnackBar.show(binding.root, it.message!!)
                    if (it is UserRecoverableAuthIOException) {
                        startActivityForResult(it.intent, AuthorizationFragment.REQUEST_AUTHORIZATION)
                    } else
                        Toast.makeText(context, it.toString(), Toast.LENGTH_LONG).show()
                }.execute(nextTokenPage)
            }
        })

        playListsDialogFragment.onSelectPlaylist = {
            if (Preferences.getSelectedPlayList(activity!!).isNullOrEmpty())
                alterSelectionPlayListButton()

            Preferences.setSelectedPlayList(context!!, it)
            binding.tvPlayListName.text = it.snippet.title
            loadVideoItems(it)
        }

        playListsDialogFragment.showPlayLists(activity!!.supportFragmentManager)
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
