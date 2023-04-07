package com.youtubemusic.core.data.di

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class YouTubeModule {

    @Provides
    fun provideHttpTransport(): HttpTransport {
        return AndroidHttp.newCompatibleTransport()
    }

    @Provides
    fun provideJsonFactory(): JsonFactory {
        return JacksonFactory.getDefaultInstance()
    }
}