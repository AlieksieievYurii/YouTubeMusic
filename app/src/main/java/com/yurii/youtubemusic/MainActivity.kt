package com.yurii.youtubemusic

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.snackbar.Snackbar
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.feature.player.PlayerControlPanelFragment
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

//TODO (Add enter & exit animation)

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ToolBarAccessor {
    private val viewModel: MainActivityViewModel by viewModels()
    private val activityMainBinding: ActivityMainBinding by viewBinding()
    private val downloadManagerBudge by lazy { BadgeDrawable.create(this) }
    private val navController by lazy { (supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment).navController }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(activityMainBinding.toolbar)
        activityMainBinding.bottomNavigationView.setupWithNavController(navController)
        activityMainBinding.toolbar.setupWithNavController(
            navController, AppBarConfiguration(setOf(R.id.fragment_youtube_music, R.id.fragment_saved_music))
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeEvents() }
                launch { observeNumberOfDownloadingJobs() }
            }
        }

        supportFragmentManager.beginTransaction().replace(
            R.id.player_view_holder,
            PlayerControlPanelFragment()
        ).commit()

    }

    private suspend fun observeNumberOfDownloadingJobs() {
        viewModel.numberOfDownloadingJobs.collect {
            if (it != 0) {
                downloadManagerBudge.isVisible = true
                downloadManagerBudge.number = it
            } else
                downloadManagerBudge.isVisible = false
        }
    }

    private suspend fun observeEvents() {
        viewModel.event.collectLatest {
            if (it is MainActivityViewModel.Event.MediaServiceError) {
                Snackbar.make(activityMainBinding.coordinatorLayout, it.exception.message ?: "Unknown", Snackbar.LENGTH_LONG)
                    .setAnchorView(activityMainBinding.bottomNavigationView).show()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        BadgeUtils.attachBadgeDrawable(downloadManagerBudge, activityMainBinding.toolbar, R.id.item_open_download_manager)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LAUNCH_FRAGMENT)?.let { fragmentExtra: String ->
            when (fragmentExtra) {
                EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC -> navController.navigate(R.id.fragment_saved_music)
                EXTRA_LAUNCH_FRAGMENT_YOUTUBE_MUSIC -> navController.navigate(R.id.fragment_youtube_music)
                else -> throw IllegalStateException("Cannot open fragment with $fragmentExtra")
            }

        }
    }

    companion object {
        const val EXTRA_LAUNCH_FRAGMENT = "com.yurii.youtubemusic.mainactivity.extra.launch.fragment"
        const val EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC = "com.yurii.youtubemusic.mainactivity.extra.launch.savedmusic.fragment"
        const val EXTRA_LAUNCH_FRAGMENT_YOUTUBE_MUSIC = "com.yurii.youtubemusic.mainactivity.extra.launch.youtubemusic.fragment"
    }

    override fun getToolbar(): Toolbar {
        return activityMainBinding.toolbar
    }

}
