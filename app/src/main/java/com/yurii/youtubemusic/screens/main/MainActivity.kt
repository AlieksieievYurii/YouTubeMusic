package com.yurii.youtubemusic.screens.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.yurii.youtubemusic.screens.player.PlayerControlPanelFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.QueueModesRepository
import com.yurii.youtubemusic.utilities.*
import kotlinx.coroutines.flow.collectLatest
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModel.MainActivityViewModelFactory(
            MediaServiceConnection.getInstance(
                application,
                QueueModesRepository.getInstance(application)
            )
        )
    }
    private val activityMainBinding: ActivityMainBinding by viewBinding()
    private val fragmentHelper = FragmentHelper(supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(activityMainBinding.toolbar)
        setupBottomNavigationMenu(activityMainBinding)

        fragmentHelper.showSavedMusicFragment(animated = false)

        lifecycleScope.launchWhenCreated {
            viewModel.event.collectLatest {
                if (it is MainActivityViewModel.Event.LogInEvent)
                    handleSignIn(it.account)
                else if (it is MainActivityViewModel.Event.LogOutEvent)
                    handleSignOut()

                if (it is MainActivityViewModel.Event.MediaServiceError) {
                    Snackbar.make(activityMainBinding.coordinatorLayout, it.exception.message ?: "Unknown", Snackbar.LENGTH_LONG)
                        .setAnchorView(activityMainBinding.bottomNavigationView).show()
                }
            }
        }

        supportFragmentManager.beginTransaction().replace(R.id.player_view_holder, PlayerControlPanelFragment()).commit()
    }

    private fun setupBottomNavigationMenu(activityMainBinding: ActivityMainBinding) {
        activityMainBinding.bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    private fun initAndOpenYouTubeMusicFragment() {
        try {
            val account = GoogleAccount(this).getLastSignedInAccount()
            fragmentHelper.initYouTubeMusicFragment(account)
            fragmentHelper.showYouTubeMusicFragment()
        } catch (e: IsNotSignedIn) {
            fragmentHelper.showAuthorizationFragment()
        } catch (e: DoesNotHaveRequiredScopes) {
            fragmentHelper.showAuthorizationFragment()
        }
    }

    private fun openSavedMusicFragment() {
        fragmentHelper.showSavedMusicFragment()
    }

    private fun openYouTubeMusicFragmentIfSingedInElseOpenAuthorizationFragment() {
        if (fragmentHelper.isNotYouTubeMusicFragmentInitialized())
            initAndOpenYouTubeMusicFragment()
        else
            fragmentHelper.showYouTubeMusicFragment()
    }

    private fun handleSignIn(googleSignInAccount: GoogleSignInAccount) {
        fragmentHelper.initYouTubeMusicFragment(googleSignInAccount)
        fragmentHelper.removeAuthorizationFragment()
        fragmentHelper.showYouTubeMusicFragment()
    }

    private fun handleSignOut() {
        fragmentHelper.removeYouTubeMusicFragment()
        fragmentHelper.showAuthorizationFragment()
    }

    override fun onNavigationItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.item_saved_music -> {
            openSavedMusicFragment()
            true
        }
        R.id.item_you_tube_music -> {
            openYouTubeMusicFragmentIfSingedInElseOpenAuthorizationFragment()
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
