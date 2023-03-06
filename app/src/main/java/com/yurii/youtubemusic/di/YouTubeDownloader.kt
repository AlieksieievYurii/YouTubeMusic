package com.yurii.youtubemusic.di

import com.yurii.youtubemusic.services.downloader2.DownloadManager
import com.yurii.youtubemusic.services.downloader2.DownloadManagerImpl
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