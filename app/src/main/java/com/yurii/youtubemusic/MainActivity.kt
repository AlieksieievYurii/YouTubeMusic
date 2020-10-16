package com.yurii.youtubemusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import com.yurii.youtubemusic.utilities.*
import com.yurii.youtubemusic.viewmodels.MainActivityViewModel
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var viewModel: MainActivityViewModel
    private var activeBottomMenuItem: Int = -1
    private val fragmentHelper = FragmentHelper(supportFragmentManager)

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        initActivity()
        showDefaultFragment()

        viewModel.logInEvent.observe(this, Observer {
            it.handleContent { googleAccount -> handleSignIn(googleAccount) }
        })

        viewModel.logOutEvent.observe(this, Observer {
            it.handle { handleSignOut() }
        })
    }

    private fun showDefaultFragment() {
        activeBottomMenuItem = R.id.item_saved_music
        fragmentHelper.showSavedMusicFragment(animated = false)
    }

    private fun initActivity() {
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(activityMainBinding.toolbar)
        setupBottomNavigationMenu(activityMainBinding)
    }

    private fun setupBottomNavigationMenu(activityMainBinding: ActivityMainBinding) {
        activityMainBinding.bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }


    private fun initAndOpenYouTubeMusicFragment() {
        try {
            val account = GoogleAccount.getLastSignedInAccount(this)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == activeBottomMenuItem)
            return false

        activeBottomMenuItem = item.itemId

        return when (activeBottomMenuItem) {
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LAUNCH_FRAGMENT)?.let { fragmentExtra: String ->
            onNavigationItemSelected(
                activityMainBinding.bottomNavigationView.menu.findItem(
                    when (fragmentExtra) {
                        EXTRA_LAUNCH_FRAGMENT_SAVED_MUSIC -> R.id.item_saved_music
                        EXTRA_LAUNCH_FRAGMENT_YOUTUBE_MUSIC -> R.id.item_you_tube_music
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
