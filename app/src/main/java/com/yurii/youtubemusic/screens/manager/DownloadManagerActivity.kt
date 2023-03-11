package com.yurii.youtubemusic.screens.manager

import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityDownloadManagerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadManagerActivity : AppCompatActivity() {
    private val viewModel: DownloadManagerViewModel by viewModels()
    private val binding: ActivityDownloadManagerBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.label_download_manager)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}