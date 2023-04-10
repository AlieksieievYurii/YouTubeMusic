package com.yurii.youtubemusic

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.youtubemusic.feature.saved_music.SavedMusicFragment
import com.youtubemusic.feature.youtube_downloader.AuthenticationFragment
import com.youtubemusic.feature.youtube_downloader.YouTubeMusicFragment


class FragmentHelper(private val fragmentManager: FragmentManager) {
    private var activeFragment: Fragment? = null

    var isYouTubeMusicFragmentActive = false
        get() = activeFragment is YouTubeMusicFragment
        private set

    var isAuthenticationFragmentActive = false
        get() = activeFragment is AuthenticationFragment
        private set

    fun showSavedMusicFragment(animated: Boolean = true) {
        if (activeFragment is SavedMusicFragment)
            return

        showFragment(
            fragment = fragmentManager.findFragmentByTag(TAG_SAVED_MUSIC_FRAGMENT) ?: createSavedMusicFragment(),
            animation = if (animated) R.anim.slide_in_right to R.anim.slide_out_right else null
        )
    }

    fun showYouTubeMusicFragment(animated: Boolean = true) {
        if (isYouTubeMusicFragmentActive)
            return

        fragmentManager.findFragmentByTag(TAG_AUTHENTICATION_FRAGMENT)?.let { removeFragment(it) }

        showFragment(
            fragment = fragmentManager.findFragmentByTag(TAG_YOUTUBE_MUSIC_FRAGMENT) ?: createYouTubeMusicFragment(),
            animation = if (animated) R.anim.slide_in_left to R.anim.slide_out_left else null
        )
    }

    fun showAuthenticationFragment(animated: Boolean = true) {
        if (isAuthenticationFragmentActive)
            return

        fragmentManager.findFragmentByTag(TAG_YOUTUBE_MUSIC_FRAGMENT)?.let { removeFragment(it) }

        showFragment(
            fragment = fragmentManager.findFragmentByTag(TAG_AUTHENTICATION_FRAGMENT) ?: createAuthenticationFragment(),
            animation = if (animated) R.anim.slide_in_left to R.anim.slide_out_left else null
        )
    }

    private fun createYouTubeMusicFragment(): Fragment {
        val youTubeMusicsFragment: Fragment = YouTubeMusicFragment.createInstance()

        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, youTubeMusicsFragment, TAG_YOUTUBE_MUSIC_FRAGMENT)
            hide(youTubeMusicsFragment)
        }.commitNow()

        return youTubeMusicsFragment
    }

    private fun showFragment(fragment: Fragment, animation: Pair<Int, Int>? = null) {
        fragmentManager.beginTransaction().run {
            if (animation != null)
                setCustomAnimations(animation.first, animation.second)
            activeFragment?.let { hide(it) }
            show(fragment)
        }.commit()

        activeFragment = fragment
    }

    private fun createSavedMusicFragment(): Fragment{
        val savedMusicFragment = SavedMusicFragment.createInstance()

        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, savedMusicFragment, TAG_SAVED_MUSIC_FRAGMENT)
            hide(savedMusicFragment)
        }.commitNow()

        return savedMusicFragment
    }

    private fun createAuthenticationFragment(): Fragment {
        val authenticationFragment = AuthenticationFragment.createInstance()

        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, authenticationFragment, TAG_AUTHENTICATION_FRAGMENT)
            hide(authenticationFragment)
        }.commitNow()

        return authenticationFragment
    }

    private fun removeFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .remove(fragment)
            .commitNow()
    }

    companion object {
        private const val TAG_SAVED_MUSIC_FRAGMENT = "com.yurii.youtube.music.saved.music.fragment.tag"
        private const val TAG_YOUTUBE_MUSIC_FRAGMENT = "com.yurii.youtube.music.youtube.music.fragment.tag"
        private const val TAG_AUTHENTICATION_FRAGMENT = "com.yurii.youtube.music.authentication.fragment.tag"
    }
}