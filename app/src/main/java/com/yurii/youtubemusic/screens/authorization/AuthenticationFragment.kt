package com.yurii.youtubemusic.screens.authorization

import android.content.Intent
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentAuthenticationBinding
import com.youtubemusic.core.data.repository.DoesNotHaveRequiredScopes
import com.youtubemusic.core.data.repository.GoogleAccount
import com.yurii.youtubemusic.utilities.TabFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationFragment : TabFragment<FragmentAuthenticationBinding>(
    layoutId = R.layout.fragment_authentication,
    titleStringId = R.string.label_fragment_title_youtube_musics,
    optionMenuId = null
) {

    @Inject
    lateinit var googleAccount: GoogleAccount

    private val signInWithGoogleLauncher = registerForActivityResult(GoogleAccount.singInContractor) {
        if (it != null)
            handleSignInResult(it)
        else
            handleDeclinedSignIn()
    }

    override fun onInflatedView(viewDataBinding: FragmentAuthenticationBinding) {
        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            signInWithGoogleLauncher.launch(googleAccount.signInIntent)
        }
    }

    private fun handleSignInResult(result: Intent) {
        try {
            googleAccount.signIn(result)
        } catch (error: ApiException) {
            Toast.makeText(context, "${error.message}, code:${error.statusCode}", Toast.LENGTH_LONG).show()
            binding.signInButton.isEnabled = true
        } catch (error: DoesNotHaveRequiredScopes) {
            Toast.makeText(context, "${error.message}", Toast.LENGTH_LONG).show()
            binding.signInButton.isEnabled = true
        }
    }

    private fun handleDeclinedSignIn() {
        binding.signInButton.isEnabled = true
    }

    companion object {
        fun createInstance() = AuthenticationFragment()
    }
}