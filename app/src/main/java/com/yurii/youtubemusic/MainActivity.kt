package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yurii.youtubemusic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var mainActivity: ActivityMainBinding
    private var activeBottomMenuItem: Int = -1

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
        val authorizationFragment = AuthorizationFragment.createInstance {
            openYouTubeMusic(it)
        }

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
}
