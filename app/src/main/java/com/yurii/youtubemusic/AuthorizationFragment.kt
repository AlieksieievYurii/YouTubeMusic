package com.yurii.youtubemusic

import android.app.Activity.RESULT_OK
import android.content.Intent

import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.activityViewModels
import com.google.android.gms.common.api.ApiException
import com.yurii.youtubemusic.databinding.FragmentAuthorizationBinding
import com.yurii.youtubemusic.utilities.DoesNotHaveRequiredScopes
import com.yurii.youtubemusic.utilities.GoogleAccount
import com.yurii.youtubemusic.utilities.TabFragment
import com.yurii.youtubemusic.utilities.TabParameters
import com.yurii.youtubemusic.screens.main.MainActivityViewModel

class AuthorizationFragment : TabFragment() {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var binding: FragmentAuthorizationBinding

    override fun onInflatedView(viewDataBinding: ViewDataBinding) {
        binding = viewDataBinding as FragmentAuthorizationBinding
        initSignInButton()
    }

    override fun getTabParameters(): TabParameters {
        return TabParameters(
            layoutId = R.layout.fragment_authorization,
            title = requireContext().getString(R.string.label_fragment_title_youtube_musics)
        )
    }

    private fun initSignInButton() {
        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            GoogleAccount.startSignInActivity(this)
        }
    }

    private fun handleSignInResult(result: Intent) {
        try {
            val account = GoogleAccount.obtainAccountFromIntent(result)
            mainActivityViewModel.signIn(account)
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
        fun createInstance(): AuthorizationFragment = AuthorizationFragment()
    }
}