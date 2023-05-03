package com.yurii.youtubemusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.feature.player.PlayerControlPanelFragment
import com.yurii.youtubemusic.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.lang.ref.WeakReference

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ToolBarAccessor {
    private val viewModel: MainActivityViewModel by viewModels()
    private val activityMainBinding: ActivityMainBinding by viewBinding()
    internal val navController by lazy { (supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment).navController }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(activityMainBinding.toolbar)
        setupWithNavController()
        activityMainBinding.toolbar.setupWithNavController(
            navController, AppBarConfiguration(setOf(R.id.fragment_youtube_music, R.id.fragment_saved_music))
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeEvents() }
            }
        }

        supportFragmentManager.beginTransaction().replace(
            R.id.player_view_holder,
            PlayerControlPanelFragment()
        ).commit()

    }


    private suspend fun observeEvents() {
        viewModel.event.collectLatest {
            if (it is MainActivityViewModel.Event.MediaServiceError) {
                Snackbar.make(activityMainBinding.coordinatorLayout, it.exception.message ?: "Unknown", Snackbar.LENGTH_LONG)
                    .setAnchorView(activityMainBinding.bottomNavigationView).show()
            }
        }
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

    private fun setupWithNavController() {
        //activityMainBinding.bottomNavigationView.setupWithNavController(navController)
        val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
        activityMainBinding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id == item.itemId)
                return@setOnItemSelectedListener false

            when (item.itemId) {
                R.id.fragment_saved_music -> {
                    builder.setEnterAnim(R.anim.slide_from_left_to_center)
                        .setExitAnim(R.anim.slide_from_center_to_right)
                        .setPopEnterAnim(R.anim.slide_from_right_to_center)
                        .setPopExitAnim(R.anim.slide_from_center_to_left)
                }

                R.id.fragment_youtube_music -> {
                    builder.setEnterAnim(R.anim.slide_from_right_to_center)
                        .setExitAnim(R.anim.slide_from_center_to_left)
                        .setPopEnterAnim(R.anim.slide_from_left_to_center)
                        .setPopExitAnim(R.anim.slide_from_center_to_right)
                }
            }
            if (item.order and Menu.CATEGORY_SECONDARY == 0) {
                builder.setPopUpTo(
                    navController.graph.findStartDestination().id,
                    inclusive = false,
                    saveState = true
                )
            }
            navController.navigate(item.itemId, null, builder.build())
            true
        }
        val weakReference = WeakReference(activityMainBinding.bottomNavigationView)
        navController.addOnDestinationChangedListener(
            object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
                    val view = weakReference.get()
                    if (view == null) {
                        navController.removeOnDestinationChangedListener(this)
                        return
                    }
                    view.menu.forEach { item ->
                        if (destination.hierarchy.any { it.id == item.itemId }) {
                            item.isChecked = true
                        }
                    }
                }
            })
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
