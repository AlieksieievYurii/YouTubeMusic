package com.youtubemusic.feature.youtube_downloader.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.youtubemusic.core.data.repository.DoesNotHaveRequiredScopes
import com.youtubemusic.core.data.repository.GoogleAccount
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentAuthenticationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationFragment : Fragment(R.layout.fragment_authentication) {
    private val binding: FragmentAuthenticationBinding by viewBinding()

    @Inject
    lateinit var googleAccount: GoogleAccount

    private val signInWithGoogleLauncher = registerForActivityResult(GoogleAccount.singInContractor) {
        if (it != null)
            handleSignInResult(it)
        else
            handleDeclinedSignIn()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            signInWithGoogleLauncher.launch(googleAccount.signInIntent)
        }
    }

    private fun handleSignInResult(result: Intent) {
        try {
            googleAccount.signIn(result)
            findNavController().popBackStack()
        } catch (error: ApiException) {
            Firebase.crashlytics.recordException(error)
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
}