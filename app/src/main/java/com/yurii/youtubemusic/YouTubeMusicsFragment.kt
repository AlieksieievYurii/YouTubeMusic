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
import com.google.api.services.youtube.model.PlaylistItem
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListsDialogFragment
import com.yurii.youtubemusic.dialogplaylists.PlayListsResultCallBack
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.YouTubeService
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException

class YouTubeMusicsFragment : Fragment(), Loader {
    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videoItemsHandler: VideoItemsHandler
    private lateinit var mCredential: GoogleAccountCredential

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        binding.btnSelectPlayList.setOnClickListener {
            selectPlayList()
        }
        videoItemsHandler = VideoItemsHandler(binding.videos, this)
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
            loadVideoItems(it)
        }

        playListsDialogFragment.showPlayLists(activity!!.supportFragmentManager)
    }

    override fun onStart() {
        super.onStart()
        val playList = Preferences.getSelectedPlayList(context!!)
        videoItemsHandler.onStart()
        playList?.let {
            loadVideoItems(it)
        } ?: showOptionToSelectPlayListFirstTime()
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
        }
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
