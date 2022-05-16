package com.yurii.youtubemusic.screens.youtube


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.databinding.FragmentYouTubeMusicsBinding
import com.yurii.youtubemusic.utilities.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.screens.main.MainActivityViewModel
import kotlinx.coroutines.flow.collectLatest
import java.lang.IllegalArgumentException


class YouTubeMusicsFragment2 : TabFragment<FragmentYouTubeMusicsBinding>(
    layoutId = R.layout.fragment_you_tube_musics,
    titleStringId = R.string.label_fragment_title_youtube_musics,
    optionMenuId = R.menu.youtube_music_fragment_menu
) {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val viewModel: YouTubeMusicViewModel2 by viewModels { YouTubeMusicViewModel2.Factory(requireContext(), getGoogleSignInAccount()) }

    private val listAdapter = VideoItemsListAdapter()

    override fun onClickOption(id: Int) {
        when (id) {
            R.id.item_log_out -> {
                mainActivityViewModel.logOut()
            }
        }
    }

    override fun onInflatedView(viewDataBinding: FragmentYouTubeMusicsBinding) {
        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.videoItems.collectLatest {
                listAdapter.submitData(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.status.collectLatest {
                Log.i("TEST", it.toString())
            }
        }
        viewModel.test()
    }

    private fun getGoogleSignInAccount(): GoogleSignInAccount {
        return this.arguments?.getParcelable(GOOGLE_SIGN_IN) ?: throw IllegalArgumentException("GoogleSignIn is required!")
    }

    companion object {
        private const val GOOGLE_SIGN_IN = "com.yurii.youtubemusic.youtubefragment.google.sign.in"

        fun createInstance(googleSignInAccount: GoogleSignInAccount): YouTubeMusicsFragment2 {
            val youTubeMusicsFragment = YouTubeMusicsFragment2()

            youTubeMusicsFragment.arguments = Bundle().apply {
                this.putParcelable(GOOGLE_SIGN_IN, googleSignInAccount)
            }

            return youTubeMusicsFragment
        }
    }
}
