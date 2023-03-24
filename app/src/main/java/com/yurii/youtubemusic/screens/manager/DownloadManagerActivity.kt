package com.yurii.youtubemusic.screens.manager

import android.os.Bundle
import android.view.View
import android.viewbinding.library.activity.viewBinding
import android.widget.CompoundButton
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityDownloadManagerBinding
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.services.downloader.DownloadManager
import com.yurii.youtubemusic.ui.ErrorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadManagerActivity : AppCompatActivity() {
    private val viewModel: DownloadManagerViewModel by viewModels()
    private val binding: ActivityDownloadManagerBinding by viewBinding()
    private val listAdapter by lazy {
        PlaylistBindsAndJobsListAdapter(object : PlaylistBindsAndJobsListAdapter.Callback {
            override fun onAddSyncPlaylistBind() {
                AddYouTubePlaylistSynchronizationDialog.show(supportFragmentManager)
            }

            override fun cancelAllDownloading() {
                viewModel.cancelAllDownloadingJobs()
            }

            override fun onClickPlaylistSync(view: View, playlistSync: YouTubePlaylistSync) {
                PopupMenu(this@DownloadManagerActivity, view).apply {
                    menuInflater.inflate(R.menu.playlist_synchronization_item_menu, menu)
                    setOnMenuItemClickListener {
                        if (it.itemId == R.id.item_delete_media_item) {
                            viewModel.deletePlaylistSynchronization(playlistSync.youTubePlaylistId)
                            true
                        } else
                            false
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

    private val onEnablePlaylistSync = CompoundButton.OnCheckedChangeListener { _, enabled ->
            viewModel.enableAutomationYouTubeSynchronization(enabled)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.label_download_manager)
        }

        binding.playlistsBinds.apply {
            adapter = listAdapter
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(this@DownloadManagerActivity)
        }

        binding.addPlaylistSynchronization.setOnClickListener { AddYouTubePlaylistSynchronizationDialog.show(supportFragmentManager) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.downloadingJobs.collect { listAdapter.submitDownloadingJobs(it) } }
                launch { viewModel.downloadingStatus.collect { listAdapter.updateDownloadingJobStatus(it) } }
                launch { observeEvents() }
                launch { observeYouTubePlaylistsSyncs() }
                launch { observeAutoSyncState() }
            }
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
            }
        }
    }

    private suspend fun observeYouTubePlaylistsSyncs() {
        viewModel.youTubePlaylistSyncs.collect {
            binding.layoutNoPlaylistsSynchronization.isVisible = it.isEmpty()
            listAdapter.submitPlaylistBinds(it)
        }
    }

    private suspend fun observeAutoSyncState() {
        viewModel.synchronizerState.collect {
            if (it != null)
                binding.enableAutoSync.apply {
                    setOnCheckedChangeListener(null)
                    isChecked = it
                    setOnCheckedChangeListener(onEnablePlaylistSync)
                }
        }
    }

    private fun openErrorDialog(videoId: String, errorMessage: String?) {
        ErrorDialog.create(errorMessage ?: getString(R.string.label_no_error_message)).addListeners(
            onTryAgain = { viewModel.retryDownloading(videoId) },
            onCancel = { viewModel.cancelDownloading(videoId) })
            .show(supportFragmentManager, "ErrorDialog")
    }
}