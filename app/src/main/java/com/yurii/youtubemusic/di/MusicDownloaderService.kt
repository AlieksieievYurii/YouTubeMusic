package com.yurii.youtubemusic.di

import com.yurii.youtubemusic.services.downloader.MusicDownloader
import com.yurii.youtubemusic.services.downloader.MusicDownloaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
interface MusicDownloaderService {
    @Binds
    fun provideMusicDownloader(musicDownloaderImpl: MusicDownloaderImpl): MusicDownloader
}