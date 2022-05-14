package com.yurii.youtubemusic.utilities

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.*
import com.yurii.youtubemusic.screens.authorization.AuthorizationFragment
import com.yurii.youtubemusic.screens.saved.SavedMusicFragment
import com.yurii.youtubemusic.screens.youtube.YouTubeMusicsFragment
import java.lang.Exception


class FragmentHelper(private val fragmentManager: FragmentManager) {
    private var activeFragment: TabFragment<*>? = null

    fun showSavedMusicFragment(animated: Boolean = true) {
        val savedMusicFragment: TabFragment<*> = getOrCreateSavedMusicFragment()
        replaceActivityFragmentWithSavedMusicFragment(savedMusicFragment, animated)
    }

    fun showAuthorizationFragment() {
        val authorizationFragment: TabFragment<*> = getOrCreateAuthorizationFragment()
        replaceActivityFragmentWithAuthorizationFragment(authorizationFragment)
    }

    fun removeAuthorizationFragment() {
        findAuthorizationFragment()?.also { fragment ->
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commitNow()
        }
    }

    fun initYouTubeMusicFragment(googleSignInAccount: GoogleSignInAccount) {
        raiseExceptionIfYouTubeFragmentAlreadyExists()

        val youTubeMusicsFragment: Fragment = YouTubeMusicsFragment.createInstance(googleSignInAccount)
        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, youTubeMusicsFragment, TAG_YOUTUBE_MUSIC_FRAGMENT)
            hide(youTubeMusicsFragment)
        }.commitNow()
    }

    fun isNotYouTubeMusicFragmentInitialized(): Boolean = findYouTubeMusicFragment() == null

    fun showYouTubeMusicFragment() {
        val youTubeMusicsFragment: TabFragment<*>? = findYouTubeMusicFragment()
        checkNotNull(youTubeMusicsFragment) { "YouTubeMusicFragment is not initialized!" }
        replaceActivityFragmentWithYouTubeMusicFragment(youTubeMusicsFragment)
    }

    fun removeYouTubeMusicFragment() {
        findYouTubeMusicFragment()?.also { fragment ->
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commitNow()
        }
    }

    private fun getOrCreateSavedMusicFragment(): TabFragment<*> {
        val savedMusicFragment: TabFragment<*>? = fragmentManager.findFragmentByTag(TAG_SAVED_MUSIC_FRAGMENT) as? TabFragment<*>
        return savedMusicFragment ?: createSavedMusicFragment()
    }

    private fun createSavedMusicFragment(): TabFragment<*> {
        val savedMusicFragment: TabFragment<*> = SavedMusicFragment.createInstance()

        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, savedMusicFragment, TAG_SAVED_MUSIC_FRAGMENT)
            hide(savedMusicFragment)
        }.commitNow()

        return savedMusicFragment
    }

    private fun replaceActivityFragmentWithSavedMusicFragment(fragment: TabFragment<*>, animated: Boolean) {
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
        activeFragment?.run {
            fragmentTransaction.hide(this)
        }
    }

    private fun findYouTubeMusicFragment(): TabFragment<*>? = fragmentManager.findFragmentByTag(TAG_YOUTUBE_MUSIC_FRAGMENT) as? TabFragment<*>

    private fun findAuthorizationFragment(): TabFragment<*>? = fragmentManager.findFragmentByTag(TAG_AUTHORIZATION_FRAGMENT) as? TabFragment<*>

    private fun replaceActivityFragmentWithAuthorizationFragment(authorizationFragment: TabFragment<*>) {
        fragmentManager.beginTransaction().also { fragmentTransaction ->
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
            hideActiveFragmentIfExist(fragmentTransaction)
            fragmentTransaction.show(authorizationFragment)
        }.commit()

        activeFragment = authorizationFragment
    }

    private fun replaceActivityFragmentWithYouTubeMusicFragment(youTubeMusicsFragment: TabFragment<*>) {
        fragmentManager.beginTransaction().run {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
            hideActiveFragmentIfExist(this)
            show(youTubeMusicsFragment)
        }.commit()

        activeFragment = youTubeMusicsFragment
    }


    private fun getOrCreateAuthorizationFragment(): TabFragment<*> {
        val authorizationFragment: TabFragment<*>? = findAuthorizationFragment()
        return authorizationFragment ?: createAuthorizationFragment()
    }

    private fun createAuthorizationFragment(): TabFragment<*> {
        val authorizationFragment: TabFragment<*> = AuthorizationFragment.createInstance()
        fragmentManager.beginTransaction().run {
            add(R.id.frameLayout, authorizationFragment, TAG_AUTHORIZATION_FRAGMENT)
            hide(authorizationFragment)
        }.commitNow()

        return authorizationFragment
    }

    companion object {
        private const val TAG_SAVED_MUSIC_FRAGMENT = "com.yurii.youtube.music.saved.music.fragment.tag"
        private const val TAG_YOUTUBE_MUSIC_FRAGMENT = "com.yurii.youtube.music.youtube.music.fragment.tag"
        private const val TAG_AUTHORIZATION_FRAGMENT = "com.yurii.youtube.music.authorization.fragment.tag"
    }
}