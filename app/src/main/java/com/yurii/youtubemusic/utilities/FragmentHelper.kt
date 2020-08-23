package com.yurii.youtubemusic.utilities

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.AuthorizationFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.SavedMusicFragment
import com.yurii.youtubemusic.YouTubeMusicsFragment
import java.lang.Exception


class FragmentHelper(private val fragmentManager: FragmentManager) {
    private var activeFragment: Fragment? = null

    fun showDefaultFragment() {
        showSavedMusicFragment(animated = false)
    }

    fun showSavedMusicFragment(animated: Boolean = true) {
        val savedMusicFragment: Fragment = getOrCreateSavedMusicFragment()
        replaceActivityFragmentWithSavedMusicFragment(savedMusicFragment, animated)
    }

    fun showAuthorizationFragment() {
        val authorizationFragment = getOrCreateAuthorizationFragment()
        replaceActivityFragmentWithAuthorizationFragment(authorizationFragment)
    }

    fun removeAuthorizationFragment() {
        findAuthorizationFragment()?.also { fragment ->
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }

    fun initYouTubeMusicFragment(googleSignInAccount: GoogleSignInAccount) {
        raiseExceptionIfYouTubeFragmentAlreadyExists()

        val youTubeMusicsFragment: Fragment = YouTubeMusicsFragment.createInstance(googleSignInAccount)
        fragmentManager.beginTransaction().run {
            add(
                R.id.frameLayout, youTubeMusicsFragment,
                TAG_YOUTUBE_MUSIC_FRAGMENT
            )
            hide(youTubeMusicsFragment)
        }.commit()
        fragmentManager.executePendingTransactions()
    }

    fun isNotYouTubeMusicFragmentInitialized(): Boolean = findYouTubeMusicFragment() == null

    fun showYouTubeMusicFragment() {
        val youTubeMusicsFragment: Fragment? = findYouTubeMusicFragment()
        checkNotNull(youTubeMusicsFragment) { "YouTubeMusicFragment is not initialized!" }
        replaceActivityFragmentWithYouTubeMusicFragment(youTubeMusicsFragment)
    }

    fun removeYouTubeMusicFragment() {
        findYouTubeMusicFragment()?.also { fragment ->
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
    }

    private fun getOrCreateSavedMusicFragment(): Fragment {
        val savedMusicFragment: Fragment? = fragmentManager.findFragmentByTag(TAG_SAVED_MUSIC_FRAGMENT)
        return savedMusicFragment ?: createSavedMusicFragment()
    }

    private fun createSavedMusicFragment(): Fragment {
        val savedMusicFragment = SavedMusicFragment.createInstance()

        fragmentManager.beginTransaction().run {
            add(
                R.id.frameLayout, savedMusicFragment,
                TAG_SAVED_MUSIC_FRAGMENT
            )
            hide(savedMusicFragment)
        }.commit()

        return savedMusicFragment
    }

    private fun replaceActivityFragmentWithSavedMusicFragment(fragment: Fragment, animated: Boolean) {
        fragmentManager.beginTransaction().also { fragmentTransaction ->
            if (animated)
                fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)

            hideActiveFragmentIfExist(fragmentTransaction)
            fragmentTransaction.show(fragment)
        }.commit()

        activeFragment = fragment
    }

    private fun raiseExceptionIfYouTubeFragmentAlreadyExists() {
        val youTubeMusicsFragment: Fragment? = findYouTubeMusicFragment()
        if (youTubeMusicsFragment != null)
            throw Exception("YouTubeMusicFragment is already initialized!")
    }

    private fun hideActiveFragmentIfExist(fragmentTransaction: FragmentTransaction) {
        activeFragment?.run { fragmentTransaction.hide(this) }
    }

    private fun findYouTubeMusicFragment(): Fragment? = fragmentManager.findFragmentByTag(TAG_YOUTUBE_MUSIC_FRAGMENT)

    private fun findAuthorizationFragment(): Fragment? = fragmentManager.findFragmentByTag(TAG_AUTHORIZATION_FRAGMENT)

    private fun replaceActivityFragmentWithAuthorizationFragment(authorizationFragment: Fragment) {
        fragmentManager.beginTransaction().also { fragmentTransaction ->
            hideActiveFragmentIfExist(fragmentTransaction)
            fragmentTransaction.show(authorizationFragment)
        }.commit()

        activeFragment = authorizationFragment
    }

    private fun replaceActivityFragmentWithYouTubeMusicFragment(youTubeMusicsFragment: Fragment) {
        fragmentManager.beginTransaction().run {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
            hideActiveFragmentIfExist(this)
            show(youTubeMusicsFragment)
        }.commit()

        activeFragment = youTubeMusicsFragment
    }


    private fun getOrCreateAuthorizationFragment(): Fragment {
        val authorizationFragment: Fragment? = findAuthorizationFragment()
        return authorizationFragment ?: createAuthorizationFragment()
    }

    private fun createAuthorizationFragment(): Fragment {
        val authorizationFragment = AuthorizationFragment.createInstance()
        fragmentManager.beginTransaction().run {
            add(
                R.id.frameLayout, authorizationFragment,
                TAG_AUTHORIZATION_FRAGMENT
            )
            hide(authorizationFragment)
        }.commit()

        return authorizationFragment
    }

    companion object {
        private const val TAG_SAVED_MUSIC_FRAGMENT = "com.yurii.youtubemusic.saved.music.fragment.tag"
        private const val TAG_YOUTUBE_MUSIC_FRAGMENT = "youtube.music.fragment.tag"
        private const val TAG_AUTHORIZATION_FRAGMENT = "com.yurii.youtubemusic.authorization.fragment.tag"
    }
}