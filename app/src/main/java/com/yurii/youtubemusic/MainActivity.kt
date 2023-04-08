package com.yurii.youtubemusic

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.feature.player.PlayerControlPanelFragment
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener, ToolBarAccessor {
    private val viewModel: MainActivityViewModel by viewModels()
    private val activityMainBinding: ActivityMainBinding by viewBinding()
    private val fragmentHelper = FragmentHelper(supportFragmentManager)
    private val downloadManagerBudge by lazy { BadgeDrawable.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(activityMainBinding.toolbar)
        activityMainBinding.bottomNavigationView.setOnItemSelectedListener(this)

        fragmentHelper.showSavedMusicFragment(animated = false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeEvents() }
                launch { observeYouTubeAuthenticationState() }
                launch { observeNumberOfDownloadingJobs() }
            }
        }

        supportFragmentManager.beginTransaction().replace(R.id.player_view_holder,
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

    private suspend fun observeYouTubeAuthenticationState() {
        viewModel.isAuthenticatedAndAuthorized.collect { isAuthenticated ->
            if (fragmentHelper.isYouTubeMusicFragmentActive && !isAuthenticated)
                fragmentHelper.showAuthenticationFragment()
            else if (fragmentHelper.isAuthenticationFragmentActive && isAuthenticated)
                fragmentHelper.showYouTubeMusicFragment()
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

    override fun onNavigationItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.item_saved_music -> {
            fragmentHelper.showSavedMusicFragment()
            true
        }
        R.id.item_you_tube_music -> {
            if (viewModel.isAuthenticatedAndAuthorized.value)
                fragmentHelper.showYouTubeMusicFragment()
            else
                fragmentHelper.showAuthenticationFragment()
            true
        }
        else -> false
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        BadgeUtils.attachBadgeDrawable(downloadManagerBudge, activityMainBinding.toolbar, R.id.item_open_download_manager)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LAUNCH_FRAGMENT)?.let { fragmentExtra: String ->
            onNavigationItemSelected(
                activityMainBinding.bottomNavigationView.menu.findItem(
                    when (fragmentExtra) {
                        EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC -> R.id.item_saved_music.also {
                            activityMainBinding.bottomNavigationView.selectedItemId = it
                        }
                        EXTRA_LAUNCH_FRAGMENT_YOUTUBE_MUSIC -> R.id.item_you_tube_music.also {
                            activityMainBinding.bottomNavigationView.selectedItemId = it
                        }
                        else -> throw IllegalStateException("Cannot open fragment with $fragmentExtra")
                    }
                )
            )
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
