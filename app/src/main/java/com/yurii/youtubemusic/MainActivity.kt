package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yurii.youtubemusic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var mainActivity: ActivityMainBinding
    private lateinit var authorizationFragment: AuthorizationFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupBottomNavigationMenu()

        authorizationFragment = AuthorizationFragment()

        // Set first item(Saved Musics) in the menu as default
        openSavedMusicFragment()
    }

    private fun setupBottomNavigationMenu() {
        setSupportActionBar(mainActivity.contentMain.toolbar)
        mainActivity.bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }


    private fun openYouTubeMusic() {
        val youTubeMusicsFragment = YouTubeMusicsFragment()
        supportFragmentManager.beginTransaction().replace(
            R.id.frameLayout,
            youTubeMusicsFragment,
            youTubeMusicsFragment.javaClass.simpleName
        ).commit()
    }

    private fun openSavedMusicFragment() {
        val savedMusicFragment = SavedMusicFragment()
        supportFragmentManager.beginTransaction().replace(
            R.id.frameLayout,
            savedMusicFragment,
            savedMusicFragment.javaClass.simpleName
        ).commit()
    }

    private fun openAuthorizationFragment() {

        authorizationFragment.signInCallBack = {
            openYouTubeMusic()
        }

        supportFragmentManager.beginTransaction().replace(
            R.id.frameLayout,
            authorizationFragment,
            authorizationFragment.javaClass.simpleName
        ).commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_saved_music -> {
                openSavedMusicFragment(); true
            }
            R.id.item_you_tube_music -> {
                openAuthorizationFragment(); true
            }
            else -> false
        }

    }
}
