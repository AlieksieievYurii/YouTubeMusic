package com.yurii.youtubemusic.screens.authorization

import android.app.Activity.RESULT_OK
import android.content.Intent

import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentAuthenticationBinding
import com.yurii.youtubemusic.source.DoesNotHaveRequiredScopes
import com.yurii.youtubemusic.source.GoogleAccount
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

    override fun onInflatedView(viewDataBinding: FragmentAuthenticationBinding) {
        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            googleAccount.startSignInActivity(this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GoogleAccount.REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK)
                    handleSignInResult(data!!)
                else
                    handleDeclinedSignIn()
            }
        }
    }

    companion object {
        fun createInstance() = AuthenticationFragment()
    }
}