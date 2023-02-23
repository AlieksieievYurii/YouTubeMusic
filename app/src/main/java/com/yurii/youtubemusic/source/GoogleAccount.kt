package com.yurii.youtubemusic.source

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.youtube.YouTubeScopes
import com.yurii.youtubemusic.di.YouTubeGoogleClient
import com.yurii.youtubemusic.di.YouTubeScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

class IsNotSignedIn : Exception("The user is not signed in")

class DoesNotHaveRequiredScopes : Exception("Require the scopes")

@Singleton
class GoogleAccount @Inject constructor(
    @ApplicationContext private val context: Context,
    @YouTubeScope private val scopes: Array<Scope>,
    @YouTubeGoogleClient private val googleClient: GoogleSignInClient
) {

    private val _isAuthenticatedAndAuthorized: MutableStateFlow<Boolean> = MutableStateFlow(isAuthenticatedAndAuthorized())
    val isAuthenticatedAndAuthorized = _isAuthenticatedAndAuthorized.asStateFlow()

    @Throws(ApiException::class, DoesNotHaveRequiredScopes::class)
    fun signIn(intent: Intent) {
        val completedTask = GoogleSignIn.getSignedInAccountFromIntent(intent)

        val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java) as GoogleSignInAccount
        if (GoogleSignIn.hasPermissions(account, *scopes))
            _isAuthenticatedAndAuthorized.value = true
        else
            throw DoesNotHaveRequiredScopes()
    }

    fun signOut() {
        googleClient.signOut().addOnCompleteListener {
            googleClient.revokeAccess()
        }

        _isAuthenticatedAndAuthorized.value = false
    }

    fun getGoogleAccountCredentialUsingOAuth2(): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(context, scopes.map { it.scopeUri }).also {
            it.selectedAccount = getLastSignedInAccount().account
        }
    }


    fun startSignInActivity(fragment: Fragment) {
        fragment.startActivityForResult(googleClient.signInIntent, REQUEST_SIGN_IN)
    }

    @Throws(IsNotSignedIn::class, DoesNotHaveRequiredScopes::class)
    private fun getLastSignedInAccount(): GoogleSignInAccount {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: throw IsNotSignedIn()

        if (GoogleSignIn.hasPermissions(account, Scope(YouTubeScopes.YOUTUBE_READONLY)))
            return account
        else
            throw DoesNotHaveRequiredScopes()
    }

    private fun isAuthenticatedAndAuthorized(): Boolean = try {
        getLastSignedInAccount()
        true
    } catch (_: IsNotSignedIn) {
        Timber.d("Google Account is not signed in")
        false
    } catch (_: DoesNotHaveRequiredScopes) {
        Timber.d("Google Account does not have required scope")
        false
    }


    companion object {
        const val REQUEST_SIGN_IN = 10003
    }
}