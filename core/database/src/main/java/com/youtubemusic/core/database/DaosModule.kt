package com.youtubemusic.core.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {
    @Provides
    fun provideMediaItemDao(dataBase: DataBase) = dataBase.mediaItemDao()

    @Provides
    fun providePlaylistDao(dataBase: DataBase) = dataBase.playlistDao()

    @Provides
    fun provideYouTubePlaylistSynchronizationDao(dataBase: DataBase) = dataBase.playlistSyncBindDao()
}