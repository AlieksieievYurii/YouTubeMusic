package com.yurii.youtubemusic.screens.youtube


import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.databinding.FragmentYoutubeMusicBinding
import com.yurii.youtubemusic.utilities.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.main.MainActivityViewModel
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.yurii.youtubemusic.screens.youtube.playlists.PlaylistsDialogFragment
import com.yurii.youtubemusic.ui.ErrorDialog
import com.yurii.youtubemusic.ui.SelectCategoriesDialog
import com.yurii.youtubemusic.ui.showDeletionDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class YouTubeMusicFragment : TabFragment<FragmentYoutubeMusicBinding>(
    layoutId = R.layout.fragment_youtube_music,
    titleStringId = R.string.label_fragment_title_youtube_musics,
    optionMenuId = R.menu.youtube_music_fragment_menu
) {
    sealed class ViewState {
        object NoSelectedPlaylist : ViewState()
        object VideosLoaded : ViewState()
        object Loading : ViewState()
        object EmptyList : ViewState()
        object Error : ViewState()
    }

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    @Inject
    lateinit var assistedFactory: YouTubeMusicViewModelAssistedFactory

    private val viewModel: YouTubeMusicViewModel by viewModels {
        YouTubeMusicViewModel.Factory(assistedFactory, requireArguments().requireParcelable(GOOGLE_SIGN_IN))
    }

    private val listAdapter: VideoItemsListAdapter by lazy {
        VideoItemsListAdapter(object : VideoItemsListAdapter.Callback {
            override fun getItemStatus(videoItem: VideoItem): VideoItemStatus = viewModel.getItemStatus(videoItem)
            override fun onDownload(videoItem: VideoItem) = viewModel.download(videoItem)
            override fun onDownloadAndAssignedCategories(videoItem: VideoItem) = viewModel.openCategorySelectorFor(videoItem)
            override fun onCancelDownloading(videoItem: VideoItem) = viewModel.cancelDownloading(videoItem)
            override fun onDelete(videoItem: VideoItem) = showConfirmationDialogToDeleteVideoItem(videoItem)
            override fun onShowErrorDetail(videoItem: VideoItem) = viewModel.showFailedItemDetails(videoItem)

        })
    }

    override fun onClickOption(id: Int) {
        when (id) {
            R.id.item_log_out -> viewModel.signOut()
        }
    }

    override fun onInflatedView(viewDataBinding: FragmentYoutubeMusicBinding) {
        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            launch { startHandlingCurrentPlaylist() }
            launch { viewModel.videoItems.collectLatest { listAdapter.submitData(it) } }
            launch { startHandlingListLoadState() }
            launch { startHandlingEvents() }
            launch { viewModel.videoItemStatus.collectLatest { listAdapter.updateItem(it) } }
        }

        binding.apply {
            btnTryAgain.setOnClickListener { listAdapter.retry() }
            btnSelectPlayList.setOnClickListener { openDialogToSelectPlaylist(viewModel.currentPlaylistId.value) }
            btnSelectPlayListFirst.setOnClickListener { openDialogToSelectPlaylist(null) }
            refresh.setOnRefreshListener { listAdapter.refresh() }
        }
    }

    private suspend fun startHandlingEvents() = viewModel.event.collectLatest { event ->
        when (event) {
            is YouTubeMusicViewModel.Event.SignOut -> mainActivityViewModel.logOut()
            is YouTubeMusicViewModel.Event.ShowFailedVideoItem -> showFailedVideoItem(event.videoItem, event.error)
            is YouTubeMusicViewModel.Event.OpenCategoriesSelector -> showDialogToSelectCategories(
                event.videoItem,
                event.allCustomCategories
            )
        }
    }

    private fun showDialogToSelectCategories(videoItem: VideoItem, allCustomCategories: List<Category>) {
        SelectCategoriesDialog(requireContext(), allCustomCategories, emptyList()) { categories ->
            viewModel.download(videoItem, categories)
        }.show()
    }

    private fun showConfirmationDialogToDeleteVideoItem(videoItem: VideoItem) {
        showDeletionDialog(requireContext(), R.string.dialog_confirm_deletion_music_title, R.string.dialog_confirm_deletion_music_message) {
            viewModel.delete(videoItem)
        }
    }

    private fun showFailedVideoItem(videoItem: VideoItem, error: Exception?) {
        ErrorDialog.create(videoItem, error).addListeners(
            onTryAgain = { viewModel.tryToDownloadAgain(videoItem) },
            onCancel = { viewModel.cancelDownloading(videoItem) })
            .show(requireActivity().supportFragmentManager, "ErrorDialog")
    }

    private suspend fun startHandlingCurrentPlaylist() = viewModel.currentPlaylistId.collectLatest {
        binding.apply {
            if (it != null) {
                viewState = ViewState.Loading
                tvPlayListName.text = it.name
            } else {
                viewState = ViewState.NoSelectedPlaylist
            }
        }
    }

    private suspend fun startHandlingListLoadState() = listAdapter.loadStateFlow.collectLatest {
        when (it.refresh) {
            is LoadState.Loading -> if (!binding.refresh.isRefreshing) binding.viewState = ViewState.Loading
            is LoadState.NotLoading -> {
                binding.refresh.isRefreshing = false
                if (viewModel.currentPlaylistId.value != null)
                    binding.viewState = ViewState.VideosLoaded
            }
            is LoadState.Error -> {
                binding.refresh.isRefreshing = false
                val loadStateError = it.refresh as LoadState.Error
                if (loadStateError.error is EmptyListException)
                    binding.viewState = ViewState.EmptyList
                else {
                    binding.viewState = ViewState.Error
                    binding.error.text = loadStateError.error.message ?: "None"
                }
            }
        }
    }

    private fun openDialogToSelectPlaylist(currentPlaylist: Playlist?) = PlaylistsDialogFragment.show(
        requireActivity().supportFragmentManager,
        viewModel.youTubeAPI, currentPlaylist, viewModel::setPlaylist
    )

    companion object {
        private const val GOOGLE_SIGN_IN = "com.yurii.youtubemusic.youtubefragment.google.sign.in"

        fun createInstance(googleSignInAccount: GoogleSignInAccount): YouTubeMusicFragment {
            val youTubeMusicsFragment = YouTubeMusicFragment()

            youTubeMusicsFragment.arguments = Bundle().apply {
                this.putParcelable(GOOGLE_SIGN_IN, googleSignInAccount)
            }

            return youTubeMusicsFragment
        }
    }
}
