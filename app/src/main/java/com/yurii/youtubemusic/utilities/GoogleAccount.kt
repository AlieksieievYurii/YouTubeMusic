package com.yurii.youtubemusic.utilities

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.youtube.YouTubeScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

class IsNotSignedIn : Exception("The user is not signed in")

class DoesNotHaveRequiredScopes : Exception("Require the scopes")

@Singleton
class GoogleAccount @Inject constructor(@ApplicationContext private val context: Context) {
    private val scopes = arrayOf(Scope(YouTubeScopes.YOUTUBE_READONLY))
    private val googleSignInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestScopes(scopes.first(), *scopes)
        .requestEmail()
        .build()

    @Throws(ApiException::class, DoesNotHaveRequiredScopes::class)
    fun obtainAccountFromIntent(intent: Intent): GoogleSignInAccount {
        val completedTask = GoogleSignIn.getSignedInAccountFromIntent(intent)

        val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java) as GoogleSignInAccount
        if (GoogleSignIn.hasPermissions(account, *scopes))
            return account
        else
            throw DoesNotHaveRequiredScopes()
    }

    fun getGoogleAccountCredentialUsingOAuth2(googleSignInAccount: GoogleSignInAccount): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(context, scopes.map { it.scopeUri }).also {
            it.selectedAccount = googleSignInAccount.account
        }
    }


    fun startSignInActivity(fragment: Fragment) {
        val client = getClient()
        fragment.startActivityForResult(client.signInIntent, REQUEST_SIGN_IN)
    }

    fun signOut() {
        val client = getClient()
        client.signOut().addOnCompleteListener {
            client.revokeAccess()
        }
    }

    @Throws(IsNotSignedIn::class, DoesNotHaveRequiredScopes::class)
    fun getLastSignedInAccount(): GoogleSignInAccount {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: throw IsNotSignedIn()

        if (GoogleSignIn.hasPermissions(account, Scope(YouTubeScopes.YOUTUBE_READONLY)))
            return account
        else
            throw DoesNotHaveRequiredScopes()
    }

    private fun getClient(): GoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    companion object {
        const val REQUEST_SIGN_IN = 10003
    }
}