package com.yurii.youtubemusic.di

import android.content.Context
import androidx.work.WorkManager
import com.github.kiulian.downloader.YoutubeDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainScope

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providesWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    fun provideYouTubeDownloader() = YoutubeDownloader()

    @Provides
    @MainScope
    fun provideMainScope(): CoroutineScope = MainScope()
}