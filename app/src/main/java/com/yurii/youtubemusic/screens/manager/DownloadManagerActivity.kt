package com.yurii.youtubemusic.screens.manager

import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityDownloadManagerBinding
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

            }

            override fun cancelAllDownloading() {
                viewModel.cancelAllDownloadingJobs()
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.downloadingJobs.collect { listAdapter.submitDownloadingJobs(it) } }
                launch { viewModel.downloadingStatus.collect { listAdapter.updateDownloadingJobStatus(it) } }
                launch {
                    viewModel.events.collect {
                        when (it) {
                            is DownloadManagerViewModel.Event.OpenFailedJobError -> openErrorDialog(it.videoId, it.error)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun openErrorDialog(videoId: String, errorMessage: String?) {
        ErrorDialog.create(errorMessage ?: getString(R.string.label_no_error_message)).addListeners(
            onTryAgain = { viewModel.retryDownloading(videoId) },
            onCancel = { viewModel.cancelDownloading(videoId) })
            .show(supportFragmentManager, "ErrorDialog")
    }
}