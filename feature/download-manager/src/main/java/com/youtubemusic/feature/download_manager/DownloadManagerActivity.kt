package com.youtubemusic.feature.download_manager

import android.os.Bundle
import android.view.View
import android.viewbinding.library.activity.viewBinding
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.ui.ErrorDialog
import com.youtubemusic.core.common.ui.SelectPlaylistsDialog
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.feature.download_manager.databinding.ActivityDownloadManagerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadManagerActivity : AppCompatActivity() {
    internal val viewModel: DownloadManagerViewModel by viewModels()
    private val binding: ActivityDownloadManagerBinding by viewBinding()
    private val listAdapter by lazy {
        PlaylistBindsAndJobsListAdapter(object : PlaylistBindsAndJobsListAdapter.Callback {
            override fun onAddSyncPlaylistBind() {
                AddYouTubePlaylistSynchronizationDialog.show(supportFragmentManager)
            }

            override fun cancelAllDownloading() {
                viewModel.cancelAllDownloadingJobs()
            }

            override fun onClickPlaylistSync(view: View, playlistSync: com.youtubemusic.core.model.YouTubePlaylistSync) {
                PopupMenu(this@DownloadManagerActivity, view).apply {
                    menuInflater.inflate(R.menu.playlist_synchronization_item_menu, menu)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.item_delete_playlist_sync -> {
                                viewModel.deletePlaylistSynchronization(playlistSync.youTubePlaylistId)
                                true
                            }
                            R.id.item_edit_assigned_playlists -> {
                                viewModel.editAssignedPlaylists(playlistSync)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }

            override fun openFailedJobError(itemId: String) {
                viewModel.openFailedJobError(itemId)
            }

            override fun cancelDownloading(itemId: String) {
                viewModel.cancelDownloading(itemId)
            }

            override fun getDownloadingJobState(id: String): DownloadManager.State {
                return viewModel.getDownloadingJobStatus(id)
            }
        })
    }

    private val headerAdapter: HeaderSyncConfigAdapter by lazy {
        HeaderSyncConfigAdapter(object : HeaderSyncConfigAdapter.Callback {
            override fun onSyncChange(isEnabled: Boolean) {
                viewModel.enableAutomationYouTubeSynchronization(isEnabled)
            }

            override fun onAddPlaylistSynchronization() {
                AddYouTubePlaylistSynchronizationDialog.show(supportFragmentManager)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.label_download_manager)
        }

        binding.playlistsBinds.apply {
            adapter = ConcatAdapter(headerAdapter, listAdapter)
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(PlaylistBindsAndJobsListAdapter.ItemSeparator(context))
        }

        lifecycleScope.launchWhenStarted {
            launch {
                viewModel.downloadingJobs.combine(viewModel.youTubePlaylistSyncs) { jobs, playlistSyncs ->
                    playlistSyncs.map { AdapterData.PlaylistBind(it) } to jobs.map { AdapterData.Job(it) }
                }.collectLatest { listAdapter.setDataSources(it.first, it.second) }
            }
            launch { viewModel.downloadingStatus.collectLatest { listAdapter.updateDownloadingJobStatus(it) } }
            launch { observeEvents() }
            launch { observeAutoSyncState() }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private suspend fun observeEvents() {
        viewModel.events.collect {
            when (it) {
                is DownloadManagerViewModel.Event.OpenFailedJobError -> openErrorDialog(it.videoId, it.error)
                is DownloadManagerViewModel.Event.OpenPlaylistsEditor ->
                    SelectPlaylistsDialog(this, it.allPlaylists, it.alreadySelectedPlaylists) { newPlaylists ->
                        viewModel.reassignPlaylistsForSync(it.youTubePlaylistId, newPlaylists)
                    }.show()
            }
        }
    }

    private suspend fun observeAutoSyncState() {
        viewModel.synchronizerState.collect {
            it?.let { headerAdapter.isSyncOn = it }
        }
    }

    private fun openErrorDialog(videoId: String, errorMessage: String?) {
        ErrorDialog.create(errorMessage ?: getString(com.youtubemusic.core.common.R.string.label_no_error_message)).addListeners(
            onTryAgain = { viewModel.retryDownloading(videoId) },
            onCancel = { viewModel.cancelDownloading(videoId) })
            .show(supportFragmentManager, "ErrorDialog")
    }
}