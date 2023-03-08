package com.yurii.youtubemusic.di

import com.yurii.youtubemusic.services.downloader.DownloadManager
import com.yurii.youtubemusic.services.downloader.DownloadManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface YouTubeDownloader {
    @Binds
    fun provideDownloadManager(downloadManager: DownloadManagerImpl): DownloadManager
}