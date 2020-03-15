package com.yurii.youtubemusic.fragments.authorization

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentAuthorizationBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class AuthorizationFragment : Fragment() {
    private companion object {
        const val REQUEST_ACCOUNT_PICKER = 1000
        const val REQUEST_AUTHORIZATION = 1001
        const val REQUEST_GOOGLE_PLAY_SERVICES = 1002
        const val REQUEST_PERMISSION_GET_ACCOUNTS = 1003
        const val PREF_ACCOUNT_NAME = "accountName"
    }

    private val scopes = listOf(YouTubeScopes.YOUTUBE_READONLY)

    private lateinit var binding: FragmentAuthorizationBinding
    private lateinit var mCredential: GoogleAccountCredential
    lateinit var signInCallBack: (() -> Unit)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_authorization, container, false
        )

        (activity as AppCompatActivity).supportActionBar!!.title = "YouTube Musics"
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mCredential = GoogleAccountCredential.usingOAuth2(context, scopes)
            .setBackOff(ExponentialBackOff())

        if (isSignedIn() && ::signInCallBack.isInitialized) {
            signInCallBack.invoke()
        }

        binding.signInButton.setOnClickListener {
            if (!isGooglePlayServiceAvailable())
                acquireGooglePlayServices()
            else
                signIn()
        }
    }

    private fun isSignedIn(): Boolean {
        return if (EasyPermissions.hasPermissions(context!!, Manifest.permission.GET_ACCOUNTS)) {
            val accountName: String? = activity!!.getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null)

            accountName != null
        } else false
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private fun signIn() {
        if (EasyPermissions.hasPermissions(context!!, Manifest.permission.GET_ACCOUNTS)) {
            val accountName: String? = activity!!.getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null)

            if (accountName != null) {
                mCredential.selectedAccountName = accountName

                if (::signInCallBack.isInitialized)
                    signInCallBack.invoke()
            } else
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs to access your Google account (via Contacts).",
                REQUEST_PERMISSION_GET_ACCOUNTS,
                Manifest.permission.GET_ACCOUNTS
            )
        }
    }

    private fun acquireGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode: Int = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (googleApiAvailability.isUserResolvableError(connectionStatusCode))
            showGooglePlayServiceAvailabilityErrorDialog(connectionStatusCode)
    }

    private fun showGooglePlayServiceAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val dialog: Dialog = apiAvailability.getErrorDialog(
            activity,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog.show()
    }

    private fun isGooglePlayServiceAvailable(): Boolean {
        val apiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode: Int = apiAvailability.isGooglePlayServicesAvailable(context)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    fun getCredential(): GoogleAccountCredential {
        val accountName: String? = activity!!.getPreferences(Context.MODE_PRIVATE)
            .getString(PREF_ACCOUNT_NAME, null)

        if (accountName != null) {
            mCredential.selectedAccountName = accountName
            return mCredential
        }
        throw Exception("Not found account name, Log in first")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> {
                if (requestCode != RESULT_OK) {
                    Toast.makeText(
                        context,
                        "This app requires Google Play Services. Please install " +
                                "Google Play Services on your device and relaunch this app.",
                        Toast.LENGTH_LONG
                    ).show()
                } else
                    signIn()
            }

            REQUEST_ACCOUNT_PICKER -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val accountName: String? = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        val settings: SharedPreferences =
                            activity!!.getPreferences(Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = settings.edit()
                        editor.putString(PREF_ACCOUNT_NAME, accountName)
                        editor.apply()
                        signIn()
                    }
                }
            }

            REQUEST_AUTHORIZATION -> {
                if (resultCode == RESULT_OK)
                    signIn()
            }
        }
    }
}