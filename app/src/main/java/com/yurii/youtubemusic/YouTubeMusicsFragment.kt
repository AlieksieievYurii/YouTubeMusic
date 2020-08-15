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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.playlists.PlayListsDialogFragment
import com.yurii.youtubemusic.utilities.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.playlists.PlayListsDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.videoslist.ConfirmDeletion
import com.yurii.youtubemusic.videoslist.VideosListAdapter
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideoItemChange
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideosLoader
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import kotlinx.android.synthetic.main.content_main.*
import java.lang.Exception
import java.lang.IllegalArgumentException

const val TITLE = "YouTube Musics"

class YouTubeMusicsFragment private constructor() : Fragment(), VideoItemChange, VideosLoader, ConfirmDeletion {
    private lateinit var viewModel: YouTubeMusicViewModel
    private lateinit var binding: FragmentYouTubeMusicsBinding
    private var isLoadingNewVideoItems = true
    private lateinit var videosListAdapter: VideosListAdapter
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) = viewModel.onReceive(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_you_tube_musics, container, false)

        initActionBar()
        initViewModel()
        initRecyclerView()
        setSelectPlayListListener()
        showCurrentVideoItemsIfExist()

        return binding.root
    }

    private fun initActionBar() {
        val toolbar = (activity as AppCompatActivity).toolbar
        toolbar.title = TITLE
        initToolBarMenu(toolbar)
    }

    private fun initToolBarMenu(toolbar: Toolbar) {
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.youtube_music_fragment_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_log_out -> {
                    onLogOut()
                    true
                }
                else -> false
            }
        }
    }

    private fun initViewModel() {
        val googleSignInAccount = getGoogleSignInAccount()
        val viewModelFactory = YouTubeViewModelFactory(activity!!.application, googleSignInAccount)
        viewModel = ViewModelProvider(activity!!, viewModelFactory).get(YouTubeMusicViewModel::class.java)
        viewModel.videoItemChange = this
        viewModel.videosLoader = this

        viewModel.selectedPlaylist.observe(this, Observer { playList ->
            if (playList != null)
                setPlayListTitle(playList)
            else
                showOptionToSelectPlayListFirstTime()
        })

    }

    private fun getGoogleSignInAccount(): GoogleSignInAccount {
        return this.arguments?.getParcelable(GOOGLE_SIGN_IN)
            ?: throw IllegalArgumentException("GoogleSignIn is required!")
    }

    private fun onLogOut() {
        Toast.makeText(context,  "Log out has been called", Toast.LENGTH_LONG).show()
    }

    private fun initRecyclerView() {
        videosListAdapter = VideosListAdapter(context!!, viewModel.VideoItemProvider(), this)
        val recyclerView = binding.videos

        val layoutManager = LinearLayoutManager(context)
        recyclerView.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = videosListAdapter
        }

        recyclerView.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean = viewModel.isVideoPageLast()
            override fun isLoading(): Boolean = isLoadingNewVideoItems
            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                recyclerView.post { videosListAdapter.setLoadingState() }
                viewModel.loadMoreVideos()
            }
        })
    }

    private fun setSelectPlayListListener() {
        binding.btnSelectPlayList.setOnClickListener {
            selectPlayList()
        }
    }

    private fun showCurrentVideoItemsIfExist() {
        val currentVideos = viewModel.getCurrentVideos()
        if (currentVideos.isNotEmpty()) {
            setNewVideoItems(currentVideos)
            showLoadedVideos()
        }
    }

    private fun showLoadedVideos() {
        binding.progressBar.visibility = View.GONE
        binding.videos.visibility = View.VISIBLE
    }

    private fun selectPlayList() {
        val selectionPlayListDialog = PlayListsDialogFragment.createDialog(object : PlayListsDialogInterface {
            override fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?): ICanceler {
                return viewModel.loadPlayLists(object : YouTubeObserver<PlaylistListResponse> {
                    override fun onResult(result: PlaylistListResponse) = onLoad.invoke(result)

                    override fun onError(error: Exception) {
                        ErrorSnackBar.show(binding.root, error.message!!)
                    }
                }, nextPageToken)
            }

            override fun onSelectPlaylist(selectedPlaylist: Playlist) {
                removeExitingVideos()
                viewModel.setNewPlayList(selectedPlaylist)
                alterSelectionPlayListButton()
            }
        }, viewModel.selectedPlaylist.value)

        selectionPlayListDialog.show(activity!!.supportFragmentManager, "SelectionPlayListFragment")
    }

    private fun removeExitingVideos() {
        videosListAdapter.removeAllVideoItem()
        showLoadingProgress()
    }

    private fun showLoadingProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.videos.visibility = View.GONE
    }

    override fun onChangeProgress(videoItem: VideoItem, progress: Progress) {
        videosListAdapter.setProgress(videoItem, progress)
    }

    override fun onDownloadingFinished(videoItem: VideoItem) {
        videosListAdapter.setFinishedState(videoItem)
    }

    private fun setNewVideoItems(videoItems: List<VideoItem>) {
        videosListAdapter.setNewVideoItems(videoItems)
        isLoadingNewVideoItems = false
    }

    private fun showEmptyPlaylistLabel() {
        binding.apply {
            labelEmptyPlaylist.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }

    private fun addMoreVideoItems(videoItems: List<VideoItem>) {
        if (isLoadingNewVideoItems) {
            videosListAdapter.removeLoadingState()
            isLoadingNewVideoItems = false
        }
        videosListAdapter.addVideoItems(videoItems)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(broadcastReceiver, IntentFilter().also {
            it.addAction(MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION)
            it.addAction(MusicDownloaderService.DOWNLOADING_FINISHED_ACTION)
        })
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(broadcastReceiver)
    }

    private fun alterSelectionPlayListButton(): Unit =
        binding.let {
            it.layoutSelectionFirstPlaylist.visibility = View.GONE
            it.layoutSelectionPlaylist.visibility = View.VISIBLE
            it.labelEmptyPlaylist.visibility = View.GONE
        }

    private fun showOptionToSelectPlayListFirstTime() {
        binding.btnSelectPlayListFirst.setOnClickListener { selectPlayList() }

        binding.apply {
            layoutSelectionFirstPlaylist.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            layoutSelectionPlaylist.visibility = View.GONE
        }
    }

    private fun setPlayListTitle(playList: Playlist) {
        binding.tvPlayListName.text = playList.snippet.title
    }

    override fun onResult(newVideos: List<VideoItem>) {
        if (videosListAdapter.isVideosEmpty() && newVideos.isEmpty())
            showEmptyPlaylistLabel()
        else if (videosListAdapter.isVideosEmpty()) {
            setNewVideoItems(newVideos)
            showLoadedVideos()
        } else
            addMoreVideoItems(newVideos)
    }

    override fun onError(error: Exception) {
        ErrorSnackBar.show(binding.root, error.message!!)
    }

    override fun requestConfirmDeletion(onConfirm: () -> Unit) = ConfirmDeletionDialog(onConfirm).show(fragmentManager!!, "fe")

    companion object {
        private const val GOOGLE_SIGN_IN = "com.yurii.youtubemusic.youtubefragment.google.sign.in"

        fun createInstance(googleSignInAccount: GoogleSignInAccount): YouTubeMusicsFragment {
            val youTubeMusicsFragment = YouTubeMusicsFragment()
            youTubeMusicsFragment.arguments = Bundle().apply {
                this.putParcelable(GOOGLE_SIGN_IN, googleSignInAccount)
            }

            return youTubeMusicsFragment
        }
    }

}
