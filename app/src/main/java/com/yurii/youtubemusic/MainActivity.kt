package com.yurii.youtubemusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.navigation.NavigationView
import com.yurii.youtubemusic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var mainActivity: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupNavigationMenu()

        // Set first item(Saved Musics) in the menu as default
        onNavigationItemSelected(mainActivity.navigationView.menu.getItem(0))
    }

    private fun setupNavigationMenu() {
        setSupportActionBar(mainActivity.contentMain.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            mainActivity.drawerLayout,
            mainActivity.contentMain.toolbar,
            0,
            0
        )

        mainActivity.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        mainActivity.navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (mainActivity.drawerLayout.isDrawerOpen(GravityCompat.START))
            mainActivity.drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        mainActivity.drawerLayout.closeDrawers()

        return when (item.itemId) {
            R.id.item_saved_music -> {
                val savedMusicFragment = SavedMusicFragment()
                supportFragmentManager.beginTransaction().replace(
                    R.id.frameLayout,
                    savedMusicFragment,
                    savedMusicFragment.javaClass.simpleName
                ).commit()
                true
            }
            R.id.item_you_tube_music -> {
                val youTubeMusicsFragment = YouTubeMusicsFragment()
                supportFragmentManager.beginTransaction().replace(
                    R.id.frameLayout,
                    youTubeMusicsFragment,
                    youTubeMusicsFragment.javaClass.simpleName
                ).commit()
                true
            }

            else -> false
        }

    }
}
