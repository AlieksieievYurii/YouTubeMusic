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

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var mainActivity: ActivityMainBinding
    private var activeBottomMenuItem: Int = -1
    private val broadCastReceiver = MyBroadCastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setSupportActionBar(mainActivity.contentMain.toolbar)
        setupBottomNavigationMenu()

        if (savedInstanceState == null)
            openSavedMusicFragment()
    }

    private fun setupBottomNavigationMenu() {
        mainActivity.bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }


    private fun openYouTubeMusic(googleSignInAccount: GoogleSignInAccount) {
        val youTubeMusicsFragment = YouTubeMusicsFragment.createInstance(googleSignInAccount)

        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left).replace(
            R.id.frameLayout,
            youTubeMusicsFragment,
            youTubeMusicsFragment.javaClass.simpleName
        ).commit()
    }

    private fun openSavedMusicFragment(animation: Boolean = false) {
        val savedMusicFragment = SavedMusicFragment()

        supportFragmentManager.beginTransaction().apply {
            if (animation)
                this.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)

            this.replace(
                R.id.frameLayout,
                savedMusicFragment,
                savedMusicFragment.javaClass.simpleName
            )
            this.commit()
        }

    }

    private fun openAuthorizationFragment() {
        val authorizationFragment = AuthorizationFragment.createInstance()

        supportFragmentManager.beginTransaction().replace(
            R.id.frameLayout,
            authorizationFragment,
            authorizationFragment.javaClass.simpleName
        ).commit()
    }

    private fun openYouTubeMusicIfUserSignedIn() {
        try {
            val account = GoogleAccount.getLastSignedInAccount(this)
            openYouTubeMusic(account)
        } catch (e: IsNotSignedIn) {
            openAuthorizationFragment()
        } catch (e: DoesNotHaveRequiredScopes) {
            openAuthorizationFragment()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == activeBottomMenuItem)
            return false

        activeBottomMenuItem = item.itemId

        return when (activeBottomMenuItem) {
            R.id.item_saved_music -> {
                openSavedMusicFragment(animation = true)
                true
            }
            R.id.item_you_tube_music -> {
                openYouTubeMusicIfUserSignedIn()
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
                ACTION_USER_SIGNED_IN -> openYouTubeMusic(intent.getParcelableExtra(EXTRA_GOOGLE_SIGN_IN_ACCOUNT) as GoogleSignInAccount)
                ACTION_USER_SIGNED_OUT -> openAuthorizationFragment()
            }
        }
    }
}
