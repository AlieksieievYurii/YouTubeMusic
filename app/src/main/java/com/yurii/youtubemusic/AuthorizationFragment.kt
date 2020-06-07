package com.yurii.youtubemusic

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.youtube.YouTubeScopes
import com.yurii.youtubemusic.databinding.FragmentAuthorizationBinding

class AuthorizationFragment : Fragment() {
    companion object {
        const val REQUEST_SIGN_IN = 10003

        fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account?.let {
                if (GoogleSignIn.hasPermissions(account, Scope(YouTubeScopes.YOUTUBE_READONLY)))
                    return account
            }
            return null
        }
    }

    private lateinit var binding: FragmentAuthorizationBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    var signInCallBack: ((account: GoogleSignInAccount) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_authorization, container, false)

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(YouTubeScopes.YOUTUBE))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity!!, gso)

        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            signIn()
        }
    }


    private fun signIn() {
        startActivityForResult(googleSignInClient.signInIntent, REQUEST_SIGN_IN)
    }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java) as GoogleSignInAccount
            if (GoogleSignIn.hasPermissions(account, Scope(YouTubeScopes.YOUTUBE_READONLY)))
                signInCallBack?.invoke(account)
        } catch (error: ApiException) {
            Toast.makeText(context, "${error.message}, code:${error.statusCode}", Toast.LENGTH_LONG).show()
            binding.signInButton.isEnabled = true
        }
    }

    private fun handleDeclinedSignIn() {
        binding.signInButton.isEnabled = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    handleSignInResult(task)
                } else
                    handleDeclinedSignIn()
            }
        }
    }
}