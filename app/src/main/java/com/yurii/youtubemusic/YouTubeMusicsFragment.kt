package com.yurii.youtubemusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.playlists.PlayListsDialogFragment
import com.yurii.youtubemusic.utilities.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.playlists.PlayListsDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.videoslist.VideosListAdapter
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideoItemChange
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideosLoader
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import java.lang.Exception
import java.lang.IllegalArgumentException


class YouTubeMusicsFragment : Fragment(), VideoItemChange {
    private lateinit var mViewModel: YouTubeMusicViewModel
    private lateinit var mBinding: FragmentYouTubeMusicsBinding
    private lateinit var mRecyclerView: RecyclerView
    private var isLoadingNewVideoItems = true
    private lateinit var mVideosListAdapter: VideosListAdapter
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) = mViewModel.onReceive(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val googleSignInAccount = this.arguments?.getParcelable<GoogleSignInAccount>(GOOGLE_SIGN_IN) ?: throw IllegalArgumentException("GoogleSignIn is required!")

        mViewModel = ViewModelProvider(activity!!, YouTubeViewModelFactory(activity!!.application, googleSignInAccount))
            .get(YouTubeMusicViewModel::class.java)

        mViewModel.mVideoItemChange = this
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)
        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"
        initRecyclerView()

        mBinding.btnSelectPlayList.setOnClickListener { selectPlayList() }


        mViewModel.selectedPlaylist.observe(this, Observer { playList ->
            if (playList != null)
                mBinding.tvPlayListName.text = playList.snippet.title
            else
                showOptionToSelectPlayListFirstTime()
        })

        val currentVideos = mViewModel.getCurrentVideos()
        if (currentVideos.isNotEmpty()) {
            setNewVideoItems(currentVideos)
            mBinding.progressBar.visibility = View.GONE
            mBinding.videos.visibility = View.VISIBLE
        }

        mViewModel.mVideosLoader = object : VideosLoader {
            override fun onResult(newVideos: List<VideoItem>) {
                if (mVideosListAdapter.isListOfVideoItemsEmpty() && newVideos.isEmpty())
                    showEmptyPlaylistLabel()
                else if (mVideosListAdapter.isListOfVideoItemsEmpty()) {
                    setNewVideoItems(newVideos)
                    mBinding.progressBar.visibility = View.GONE
                    mBinding.videos.visibility = View.VISIBLE
                } else
                    addMoreVideoItems(newVideos)
            }

            override fun onError(error: Exception) {
                ErrorSnackBar.show(mBinding.root, error.message!!)
            }
        }

        return mBinding.root
    }

    private fun initRecyclerView() {
        mVideosListAdapter = VideosListAdapter(context!!, mViewModel.VideoItemProvider())
        mRecyclerView = mBinding.videos

        val layoutManager = LinearLayoutManager(context)
        mRecyclerView.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = mVideosListAdapter
        }

        mRecyclerView.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean = mViewModel.isVideoPageLast()
            override fun isLoading(): Boolean = isLoadingNewVideoItems
            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                mRecyclerView.post { mVideosListAdapter.setLoadingState() }
                mViewModel.loadMoreVideos()
            }
        })
    }

    private fun selectPlayList() {
        val selectionPlayListDialog = PlayListsDialogFragment.createDialog(object : PlayListsDialogInterface {
            override fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?): ICanceler {
                return mViewModel.loadPlayLists(object : YouTubeObserver<PlaylistListResponse> {
                    override fun onResult(result: PlaylistListResponse) = onLoad.invoke(result)

                    override fun onError(error: Exception) {
                        ErrorSnackBar.show(mBinding.root, error.message!!)
                    }
                }, nextPageToken)
            }

            override fun onSelectPlaylist(selectedPlaylist: Playlist) {
                removeExitingVideos()
                mViewModel.setNewPlayList(selectedPlaylist)
                alterSelectionPlayListButton()
            }
        }, mViewModel.selectedPlaylist.value)

        selectionPlayListDialog.show(activity!!.supportFragmentManager, "SelectionPlayListFragment")
    }

    private fun removeExitingVideos() {
        mVideosListAdapter.removeAllVideoItem()
        mBinding.progressBar.visibility = View.VISIBLE
        mBinding.videos.visibility = View.GONE
    }

    override fun onChangeProgress(videoItem: VideoItem, progress: Progress) {
        mVideosListAdapter.setProgress(videoItem, progress)
    }

    override fun onDownloadingFinished(videoItem: VideoItem) {
        mVideosListAdapter.setFinishedState(videoItem)
    }

    private fun setNewVideoItems(videoItems: List<VideoItem>) {
        mVideosListAdapter.setNewVideoItems(videoItems)
        isLoadingNewVideoItems = false
    }

    private fun showEmptyPlaylistLabel() {
        mBinding.apply {
            labelEmptyPlaylist.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }

    private fun addMoreVideoItems(videoItems: List<VideoItem>) {
        if (isLoadingNewVideoItems) {
            mVideosListAdapter.removeLoadingState()
            isLoadingNewVideoItems = false
        }
        mVideosListAdapter.addVideoItems(videoItems)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(mBroadcastReceiver, IntentFilter().also {
            it.addAction(MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION)
            it.addAction(MusicDownloaderService.DOWNLOADING_FINISHED_ACTION)
        })
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(mBroadcastReceiver)
    }

    private fun alterSelectionPlayListButton(): Unit =
        mBinding.let {
            it.layoutSelectionFirstPlaylist.visibility = View.GONE
            it.layoutSelectionPlaylist.visibility = View.VISIBLE
            it.labelEmptyPlaylist.visibility = View.GONE
        }

    private fun showOptionToSelectPlayListFirstTime() {
        mBinding.btnSelectPlayListFirst.setOnClickListener { selectPlayList() }

        mBinding.apply {
            layoutSelectionFirstPlaylist.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            layoutSelectionPlaylist.visibility = View.GONE
        }
    }

    companion object {
        const val GOOGLE_SIGN_IN = "com.yurii.youtubemusic.youtubefragment.google.sign.in"
    }

}
