package com.youtubemusic.feature.youtube_downloader

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.youtubemusic.core.common.ToolBarAccessor


class YouTubeMusicFragment : Fragment(R.layout.fragment_youtube_music) {
    private val toolbar: Toolbar by lazy { (requireActivity() as ToolBarAccessor).getToolbar() }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a = (childFragmentManager.findFragmentById(R.id.you_tube_music_navigation) as NavHostFragment).navController
        a.addOnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id != a.graph.startDestinationId && destination.id != R.id.authenticationFragment) {
                toolbar.apply {
                    title = destination.label
                    navigationIcon = DrawerArrowDrawable(context).also { it.progress = 1f }
                    setNavigationOnClickListener {
                        a.popBackStack()
                    }
                }
            }
            else {
                toolbar.navigationIcon =  null
                toolbar.title = a.graph.findNode(a.graph.startDestinationId)!!.label
            }
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                if (a.currentDestination?.id != a.graph.startDestinationId && a.currentDestination?.id != R.id.authenticationFragment)
                    a.popBackStack()
                else
                    findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
}