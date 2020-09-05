package com.yurii.youtubemusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import com.yurii.youtubemusic.utilities.DoesNotHaveRequiredScopes
import com.yurii.youtubemusic.utilities.FragmentHelper
import com.yurii.youtubemusic.utilities.GoogleAccount
import com.yurii.youtubemusic.utilities.IsNotSignedIn

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private var activeBottomMenuItem: Int = -1
    private val broadCastReceiver = MyBroadCastReceiver()
    private val fragmentHelper = FragmentHelper(supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivity()
        showDefaultFragment()
    }

    private fun showDefaultFragment() {
        activeBottomMenuItem = R.id.item_saved_music
        fragmentHelper.showSavedMusicFragment(animated = false)
    }

    private fun initActivity() {
        val mainActivity: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(mainActivity.toolbar)
        setupBottomNavigationMenu(mainActivity)
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

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadCastReceiver, BROAD_CAST_RECEIVER_FILTER)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadCastReceiver)
    }

    companion object {
        private const val ACTION_USER_SIGNED_IN = "com.yurii.youtubemusic.mainactivity.action.signin"
        private const val ACTION_USER_SIGNED_OUT = "com.yurii.youtubemusic.mainactivity.action.signout"

        private const val EXTRA_GOOGLE_SIGN_IN_ACCOUNT = "com.yurii.youtubemusic.mainactivity.extra.googlesigninaccount"

        val BROAD_CAST_RECEIVER_FILTER = IntentFilter().apply {
            addAction(ACTION_USER_SIGNED_IN)
            addAction(ACTION_USER_SIGNED_OUT)
        }

        fun createSignInIntent(account: GoogleSignInAccount): Intent = Intent(ACTION_USER_SIGNED_IN).apply {
            putExtra(EXTRA_GOOGLE_SIGN_IN_ACCOUNT, account)
        }

        fun createSignOutIntent(): Intent = Intent(ACTION_USER_SIGNED_OUT)
    }

    private inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USER_SIGNED_IN -> {
                    val googleSignInAccount = intent.getParcelableExtra(EXTRA_GOOGLE_SIGN_IN_ACCOUNT) as GoogleSignInAccount
                    handleSignIn(googleSignInAccount)
                }
                ACTION_USER_SIGNED_OUT -> handleSignOut()
            }
        }
    }
}
