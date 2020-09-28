package com.yurii.youtubemusic

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.ViewDataBinding
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.playlists.PlayListsDialogFragment
import com.yurii.youtubemusic.utilities.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.playlists.PlayListsDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.ui.BottomNavigationBehavior
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.ErrorDialog
import com.yurii.youtubemusic.videoslist.DialogRequests
import com.yurii.youtubemusic.videoslist.VideosListAdapter
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideoItemChange
import com.yurii.youtubemusic.viewmodels.youtubefragment.VideosLoader
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.youtubefragment.YouTubeViewModelFactory
import java.lang.Exception
import java.lang.IllegalArgumentException


class YouTubeMusicsFragment : TabFragment(), VideoItemChange, VideosLoader, DialogRequests {
    private lateinit var viewModel: YouTubeMusicViewModel
    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videosListAdapter: VideosListAdapter
    private var isLoadingNewVideoItems = true
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) = viewModel.onReceive(intent)
    }

    override fun onInflatedView(viewDataBinding: ViewDataBinding) {
        binding = viewDataBinding as FragmentYouTubeMusicsBinding
        initViewModel()
        initRecyclerView()
        setSelectPlayListListener()
    }

    override fun getTabParameters(): TabParameters {
        return TabParameters(
            layoutId = R.layout.fragment_you_tube_musics,
            title = requireContext().getString(R.string.label_fragment_title_youtube_musics),
            optionMenuId = R.menu.youtube_music_fragment_menu,
            onClickOption = {
                when (it) {
                    R.id.item_log_out -> {
                        onSignOut()
                    }
                }
            }
        )
    }

    private fun initViewModel() {
        val googleSignInAccount = getGoogleSignInAccount()
        val viewModelFactory = YouTubeViewModelFactory(requireActivity().application, googleSignInAccount)
        viewModel = ViewModelProvider(this, viewModelFactory).get(YouTubeMusicViewModel::class.java)
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

    private fun onSignOut() {
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(MainActivity.createSignOutIntent())
        viewModel.signOut()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRecyclerView() {
        videosListAdapter = VideosListAdapter(requireContext(), viewModel.VideoItemProviderImplementation(), this)
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

        selectionPlayListDialog.show(requireActivity().supportFragmentManager, "SelectionPlayListFragment")
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

    override fun onDownloadingFailed(videoItem: VideoItem, error: Exception) {
        videosListAdapter.setFailedState(videoItem, error)
    }

    private fun setNewVideoItems(videoItems: List<VideoItem>) {
        videosListAdapter.setNewVideoItems(videoItems)
        slideUpBottomNavigationMenu()
        isLoadingNewVideoItems = false
    }

    private fun slideUpBottomNavigationMenu() {
        val bottomMenu = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val params = bottomMenu.layoutParams as CoordinatorLayout.LayoutParams
        val menuBehavior = params.behavior as BottomNavigationBehavior
        menuBehavior.slideUp(bottomMenu)
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
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiver, IntentFilter().also {
            it.addAction(MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION)
            it.addAction(MusicDownloaderService.DOWNLOADING_FINISHED_ACTION)
            it.addAction(MusicDownloaderService.DOWNLOADING_FAILED_ACTION)
        })
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcastReceiver)
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

    override fun requestConfirmDeletion(onConfirm: () -> Unit) {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_music_title,
            messageId = R.string.dialog_confirm_deletion_music_message,
            onConfirm = onConfirm
        ).show(requireActivity().supportFragmentManager, "RequestToDeleteFile")
    }

    override fun requestFailedToDownloadDialog(videoItem: VideoItem) {
        ErrorDialog.create(videoItem).addListeners(
            onTryAgain = {
                viewModel.startDownloadMusic(videoItem)
                videosListAdapter.setDownloadingState(videoItem)
            },
            onCancel = { videosListAdapter.setDownloadState(videoItem) }
        ).show(requireActivity().supportFragmentManager, "ErrorDialog")
    }

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
