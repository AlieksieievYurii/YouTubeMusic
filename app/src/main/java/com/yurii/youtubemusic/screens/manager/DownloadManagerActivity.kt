package com.yurii.youtubemusic.screens.manager

import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityDownloadManagerBinding
import com.yurii.youtubemusic.services.downloader.DownloadManager
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
                TODO("Not yet implemented")
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
            viewModel.downloadingJobs.collect {
                listAdapter.submitDownloadingJobs(it)
            }
        }

        lifecycleScope.launch {
            viewModel.downloadingStatus.collect {
                listAdapter.updateDownloadingJobStatus(it)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}