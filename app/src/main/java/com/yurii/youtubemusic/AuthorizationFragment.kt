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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.yurii.youtubemusic.databinding.FragmentAuthorizationBinding
import kotlinx.android.synthetic.main.content_main.*

class AuthorizationFragment private constructor() : Fragment() {
    private lateinit var binding: FragmentAuthorizationBinding

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
        val toolbar = (activity as AppCompatActivity).toolbar
        toolbar.title = "YouTube Musics"
        toolbar.menu.clear()
    }


    private fun handleSignInResult(result: Intent) {
        try {
            val account = GoogleAccount.obtainAccountFromIntent(result)
            sendBroadCastThatUserHasSignedIn(account)
        } catch (error: ApiException) {
            Toast.makeText(context, "${error.message}, code:${error.statusCode}", Toast.LENGTH_LONG).show()
            binding.signInButton.isEnabled = true
        } catch (error: DoesNotHaveRequiredScopes) {
            Toast.makeText(context, "${error.message}", Toast.LENGTH_LONG).show()
            binding.signInButton.isEnabled = true
        }
    }

    private fun sendBroadCastThatUserHasSignedIn(account: GoogleSignInAccount) {
        LocalBroadcastManager.getInstance(context!!).sendBroadcast(MainActivity.createSignInIntent(account))
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