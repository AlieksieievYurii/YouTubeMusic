package com.yurii.youtubemusic.di

import android.content.Context
import androidx.room.Room
import com.yurii.youtubemusic.db.DataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideDataBase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, DataBase::class.java, "database").build()

    @Provides
    fun provideMediaItemDao(dataBase: DataBase) = dataBase.mediaItemDao()

    @Provides
    fun providePlaylistDao(dataBase: DataBase) = dataBase.playlistDao()
}