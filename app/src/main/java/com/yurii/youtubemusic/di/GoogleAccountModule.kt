package com.yurii.youtubemusic.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.youtube.YouTubeScopes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YouTubeScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YouTubeSignInOptions

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YouTubeGoogleClient

@Module
@InstallIn(SingletonComponent::class)
class GoogleAccountModule {

    @Provides
    @YouTubeScope
    fun provideGoogleAuthorizationScope(): Array<Scope> {
        return arrayOf(Scope(YouTubeScopes.YOUTUBE_READONLY))
    }

    @Provides
    @YouTubeSignInOptions
    fun provideGoogleSingInOptions(@YouTubeScope scopes: Array<Scope>): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(scopes.first(), *scopes)
            .requestEmail()
            .build()
    }

    @Provides
    @YouTubeGoogleClient
    fun provideGoogleClient(
        @ApplicationContext context: Context,
        @YouTubeSignInOptions googleSignInOptions: GoogleSignInOptions
    ): GoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
}