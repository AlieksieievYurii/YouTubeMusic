package com.yurii.youtubemusic.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubePlaylistSynchronizationDao {

    @Insert
    suspend fun insert(vararg youTubePlaylistSync: YouTubePlaylistSyncEntity)

    @Insert
    suspend fun insertMediaItemPlaylistBinds(vararg p: YouTubePlaylistSyncCrossRefToMediaPlaylist)

    @Transaction
    suspend fun deleteYouTubePlaylistSyncAndItsRelations(youTubePlaylistId: String) {
        delete(youTubePlaylistId)
        deleteAppPlaylistsAssignments(youTubePlaylistId)
    }

    @Query("DELETE FROM you_tube_playlist_synchronization WHERE youTubePlaylistId = :youTubePlaylistId")
    suspend fun delete(youTubePlaylistId: String)

    @Query("DELETE FROM you_tube_playlist_synchronization_ref_to_media_playlist WHERE youTubePlaylistId = :youTubePlaylistId")
    suspend fun deleteAppPlaylistsAssignments(youTubePlaylistId: String)

    @Transaction
    @Query("SELECT * FROM you_tube_playlist_synchronization")
    fun getYouTubePlaylistsSyncWithBoundedMediaPlaylists(): Flow<List<YouTubePlaylistWithBoundMediaPlaylists>>
}