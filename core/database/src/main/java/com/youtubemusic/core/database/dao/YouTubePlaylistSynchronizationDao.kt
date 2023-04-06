package com.youtubemusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.youtubemusic.core.database.models.YouTubePlaylistSyncToAppPlaylistCrossRef
import com.youtubemusic.core.database.models.YouTubePlaylistSyncEntity
import com.youtubemusic.core.database.models.YouTubePlaylistWithBoundMediaPlaylists
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubePlaylistSynchronizationDao {

    @Insert
    suspend fun insert(vararg youTubePlaylistSync: YouTubePlaylistSyncEntity)

    @Insert
    suspend fun insertMediaItemPlaylistBinds(vararg p: YouTubePlaylistSyncToAppPlaylistCrossRef)

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