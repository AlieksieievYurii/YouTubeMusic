package com.yurii.youtubemusic

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.yurii.youtubemusic.databinding.FragmentAuthorizationBinding

class AuthorizationFragment private constructor() : Fragment() {
    private lateinit var binding: FragmentAuthorizationBinding
    private lateinit var signInCallBack: (account: GoogleSignInAccount) -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_authorization, container, false)

        initActionBar()
        initSignInButton()

        return binding.root
    }

    private fun initSignInButton() {
        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            GoogleAccount.startSignInActivity(this)
        }
    }

    private fun initActionBar() {
        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"
    }


    private fun handleSignInResult(result: Intent) {
        try {
            val account = GoogleAccount.obtainAccountFromIntent(result)
            signInCallBack.invoke(account)
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
        fun createInstance(onSignIn: (account: GoogleSignInAccount) -> Unit): AuthorizationFragment {
            val authorizationFragment = AuthorizationFragment()
            authorizationFragment.signInCallBack = onSignIn
            return authorizationFragment
        }
    }
}