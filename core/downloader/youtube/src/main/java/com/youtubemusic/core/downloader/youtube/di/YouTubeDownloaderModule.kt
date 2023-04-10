package com.youtubemusic.core.downloader.youtube.di

import android.content.Context
import androidx.work.WorkManager
import com.github.kiulian.downloader.YoutubeDownloader
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.downloader.youtube.DownloadManagerImpl
import dagger.Binds
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
object DependenciesProvider {
    @Provides
    @Singleton
    fun providesWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    fun provideYouTubeDownloader() = YoutubeDownloader()

    @Provides
    @MainScope
    fun provideMainScope(): CoroutineScope = MainScope()
}

@Module
@InstallIn(SingletonComponent::class)
interface YouTubeDownloader {
    @Binds
    fun provideDownloadManager(downloadManager: DownloadManagerImpl): DownloadManager
}