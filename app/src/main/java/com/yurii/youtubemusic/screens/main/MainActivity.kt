package com.yurii.youtubemusic.screens.main

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.yurii.youtubemusic.screens.player.PlayerControlPanelFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import com.yurii.youtubemusic.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private val viewModel: MainActivityViewModel by viewModels()
    private val activityMainBinding: ActivityMainBinding by viewBinding()
    private val fragmentHelper = FragmentHelper(supportFragmentManager)

    @delegate:SuppressLint("UnsafeOptInUsageError")
    private val downloadManagerBudge by lazy {
        BadgeDrawable.create(this).also {
            BadgeUtils.attachBadgeDrawable(it, toolbar, R.id.item_open_download_manager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(activityMainBinding.toolbar)
        activityMainBinding.bottomNavigationView.setOnItemSelectedListener(this)

        fragmentHelper.showSavedMusicFragment(animated = false)

        lifecycleScope.launchWhenCreated {
            launch { observeEvents() }
            launch { observeYouTubeAuthenticationState() }
            launch {
                viewModel.numberOfDownloadingJobs.collect {
                    downloadManagerBudge.number = it
                }
            }
        }

        supportFragmentManager.beginTransaction().replace(R.id.player_view_holder, PlayerControlPanelFragment()).commit()
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

}
