package com.yurii.youtubemusic

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.playlists.PlayListsDialogFragment
import com.yurii.youtubemusic.utilities.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.playlists.PlayListsDialogInterface
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.services.youtube.YouTubeObserver
import com.yurii.youtubemusic.ui.BottomNavigationBehavior
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.ErrorDialog
import com.yurii.youtubemusic.ui.SelectCategoriesDialog
import com.yurii.youtubemusic.adapters.VideosListAdapter
import com.yurii.youtubemusic.services.downloader.ServiceConnection
import com.yurii.youtubemusic.screens.main.MainActivityViewModel
import com.yurii.youtubemusic.viewmodels.VideosLoader
import com.yurii.youtubemusic.viewmodels.YouTubeMusicViewModel
import com.yurii.youtubemusic.viewmodels.YouTubeViewModelFactory
import java.lang.Exception
import java.lang.IllegalArgumentException


class YouTubeMusicsFragment : TabFragment(), VideosLoader {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val viewModel: YouTubeMusicViewModel by viewModels {
        YouTubeViewModelFactory(requireActivity().application, getGoogleSignInAccount(), ServiceLocator.providePreferences(requireContext()))
    }

    private lateinit var binding: FragmentYouTubeMusicsBinding
    private lateinit var videosListAdapter: VideosListAdapter
    private var isLoadingNewVideoItems = true
    private lateinit var downloaderServiceConnection: ServiceConnection

    override fun onInflatedView(viewDataBinding: ViewDataBinding) {
        downloaderServiceConnection = ServiceConnection(requireContext())
        downloaderServiceConnection.setCallbacks(DownloaderServiceCallBack())
        downloaderServiceConnection.connect()

        binding = viewDataBinding as FragmentYouTubeMusicsBinding
        initViewModel()
        initRecyclerView()
        setSelectPlayListListener()

        mainActivityViewModel.onMediaItemIsDeleted.observe(viewLifecycleOwner, Observer {
            videosListAdapter.setDownloadState(it)
        })
    }

    override fun getTabParameters(): TabParameters {
        return TabParameters(
            layoutId = R.layout.fragment_you_tube_musics,
            title = requireContext().getString(R.string.label_fragment_title_youtube_musics),
            optionMenuId = R.menu.youtube_music_fragment_menu,
            onClickOption = {
                when (it) {
                    R.id.item_log_out -> {
                        mainActivityViewModel.logOut()
                        viewModel.signOut()
                    }
                }
            }
        )
    }

    private fun initViewModel() {
        viewModel.videosLoader = this
        viewModel.selectedPlaylist.observe(this, Observer { playList ->
            if (playList != null)
                setPlayListTitle(playList)
            else
                showOptionToSelectPlayListFirstTime()
        })
    }

    private fun getGoogleSignInAccount(): GoogleSignInAccount {
        return this.arguments?.getParcelable(GOOGLE_SIGN_IN) ?: throw IllegalArgumentException("GoogleSignIn is required!")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRecyclerView() {
        videosListAdapter = VideosListAdapter(requireContext(), VideosListAdapterCallBacks())
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

    private inner class DownloaderServiceCallBack : ServiceConnection.CallBack {
        override fun onFinished(videoItem: VideoItem) {
            videosListAdapter.setDownloadedState(videoItem)
            mainActivityViewModel.notifyVideoItemHasBeenDownloaded(videoItem)
        }

        override fun onProgress(videoItem: VideoItem, progress: Progress) {
            videosListAdapter.setProgress(videoItem, progress)
        }

        override fun onError(videoItem: VideoItem, error: Exception) {
            videosListAdapter.setFailedState(videoItem)
        }
    }

    inner class VideosListAdapterCallBacks : VideosListAdapter.CallBack {
        override fun onDownload(videoItem: VideoItem) {
            downloaderServiceConnection.download(videoItem, emptyList())
            videosListAdapter.setDownloadingState(videoItem)
        }

        override fun onDownloadAndAddCategories(videoItem: VideoItem) {
            SelectCategoriesDialog.selectCategories(requireContext(), null) {
                downloaderServiceConnection.download(videoItem, it)
                videosListAdapter.setDownloadingState(videoItem)
            }
        }

        override fun onCancelDownload(videoItem: VideoItem) {
            downloaderServiceConnection.cancelDownloading(videoItem)
            videosListAdapter.setDownloadState(videoItem.videoId)
        }

        override fun onNotifyFailedToDownload(videoItem: VideoItem) {
            val error = downloaderServiceConnection.getError(videoItem)
            ErrorDialog.create(videoItem, error).addListeners(
                onTryAgain = {
                    downloaderServiceConnection.retryToDownload(it)
                    videosListAdapter.setDownloadingState(videoItem)
                },
                onCancel = {
                    downloaderServiceConnection.cancelDownloading(it)
                    videosListAdapter.setDownloadState(videoItem.videoId)
                }).show(requireActivity().supportFragmentManager, "ErrorDialog")
        }

        override fun onRemove(videoItem: VideoItem) {
            ConfirmDeletionDialog.create(
                titleId = R.string.dialog_confirm_deletion_music_title,
                messageId = R.string.dialog_confirm_deletion_music_message,
                onConfirm = {
                    viewModel.removeVideoItem(videoItem)
                    videosListAdapter.setDownloadState(videoItem.videoId)
                    mainActivityViewModel.notifyMediaItemHasBeenDeleted(videoItem.videoId)
                }
            ).show(requireActivity().supportFragmentManager, "RequestToDeleteFile")
        }

        override fun exists(videoItem: VideoItem) = viewModel.exists(videoItem)

        override fun isLoading(videoItem: VideoItem) = downloaderServiceConnection.isItemDownloading(videoItem)

        override fun isDownloadingFailed(videoItem: VideoItem) =  downloaderServiceConnection.isDownloadingFailed(videoItem)

        override fun getCurrentProgress(videoItem: VideoItem): Progress? = downloaderServiceConnection.getProgress(videoItem)
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
