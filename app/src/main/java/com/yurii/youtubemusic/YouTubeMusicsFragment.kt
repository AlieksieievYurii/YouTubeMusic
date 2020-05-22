package com.yurii.youtubemusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.dialogplaylists.PlayListDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.MusicDownloaderService
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.videoslist.ItemState
import com.yurii.youtubemusic.videoslist.VideoItemInterface
import com.yurii.youtubemusic.videoslist.VideosListAdapter
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideoItemChange
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideosLoader
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import java.lang.Exception


class YouTubeMusicsFragment : Fragment(), VideoItemInterface, VideoItemChange {
    private lateinit var mViewModel: YouTubeMusicViewModel
    private lateinit var mBinding: FragmentYouTubeMusicsBinding
    private lateinit var mRecyclerView: RecyclerView
    private var isLoadingNewVideoItems = true
    private val mVideosListAdapter: VideosListAdapter = VideosListAdapter(this)
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) = mViewModel.onReceive(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViewModel = ViewModelProvider(activity!!, YouTubeViewModelFactory(activity!!.application)).get(YouTubeMusicViewModel::class.java)
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
                if (mVideosListAdapter.videos.isEmpty()) {
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
        PlayListsDialogFragment().showPlayLists(activity!!.supportFragmentManager, mViewModel.selectedPlaylist.value, object : PlayListDialogInterface {
            override fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?) {
                mViewModel.loadPlayLists(object : YouTubeObserver<PlaylistListResponse> {
                    override fun onResult(result: PlaylistListResponse) = onLoad.invoke(result)

                    override fun onError(error: Exception) {
                        ErrorSnackBar.show(mBinding.root, error.message!!)
                        if (error is UserRecoverableAuthIOException) {
                            startActivityForResult(error.intent, AuthorizationFragment.REQUEST_AUTHORIZATION)
                        } else
                            Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
                    }
                }, nextPageToken)
            }

            override fun onSelectPlaylist(selectedPlaylist: Playlist) {
                removeExitingVideos()
                mViewModel.setNewPlayList(selectedPlaylist)
                alterSelectionPlayListButton()
            }
        })
    }

    private fun removeExitingVideos() {
        mVideosListAdapter.videos.clear()
        mBinding.progressBar.visibility = View.VISIBLE
        mBinding.videos.visibility = View.GONE
    }

    private fun findVideoItemView(videoItem: VideoItem, onFound: ((ItemVideoBinding) -> Unit)) {
        for (index: Int in 0 until mRecyclerView.childCount) {
            val position = mRecyclerView.getChildAdapterPosition(mRecyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION)
                continue

            if (isLoadingNewVideoItems && mVideosListAdapter.videos.lastIndex == position)
            // When new video items are loading, the last list's item is empty Video item, because it is for "loading item"
                continue

            if (mVideosListAdapter.videos[position].videoId == videoItem.videoId) {
                val viewHolder = (mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(index)) as VideosListAdapter.VideoViewHolder)
                onFound.invoke(viewHolder.videoItemVideoBinding)
            }
        }
    }

    override fun onChangeProgress(videoItem: VideoItem, progress: Int) {
        findVideoItemView(videoItem) {
            it.progressBar.progress = progress
        }
    }

    override fun onDownloadingFinished(videoItem: VideoItem) {
        findVideoItemView(videoItem) {
            it.state = ItemState.EXISTS
            it.executePendingBindings()
        }
    }

    private fun setNewVideoItems(videoItems: List<VideoItem>) {
        mVideosListAdapter.setNewVideoItems(videoItems)
        isLoadingNewVideoItems = false
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
            it.btnSelectPlayListFirst.visibility = View.GONE
            it.layoutSelectionPlaylist.visibility = View.VISIBLE
        }

    private fun showOptionToSelectPlayListFirstTime() {
        mBinding.btnSelectPlayListFirst.setOnClickListener { selectPlayList() }

        mBinding.apply {
            btnSelectPlayListFirst.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            layoutSelectionPlaylist.visibility = View.GONE
        }
    }


    override fun onItemClickDownload(videoItem: VideoItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(videoItem: VideoItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExisted(videoItem: VideoItem): Boolean = mViewModel.isExist(videoItem)

    override fun isLoading(videoItem: VideoItem): Boolean {
        return false
    }

    override fun getCurrentProgress(videoItem: VideoItem): Int {
        return 50
    }

}
