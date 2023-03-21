package com.yurii.youtubemusic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubePlaylistSynchronizationDao {

    @Insert
    suspend fun insert(vararg youTubePlaylistSync: YouTubePlaylistSyncEntity)

    @Insert
    suspend fun insertMediaItemPlaylistBinds(vararg p: YouTubePlaylistSyncCrossRefToMediaPlaylist)

    @Transaction
    @Query("SELECT * FROM you_tube_playlist_synchronization")
    fun getYouTubePlaylistsSyncWithBoundedMediaPlaylists(): Flow<List<YouTubePlaylistWithBoundMediaPlaylists>>
}