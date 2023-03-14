package com.yurii.youtubemusic.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlistEntity: PlaylistEntity): Long

    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Update
    suspend fun update(playlistEntity: PlaylistEntity)

    @Delete
    suspend fun delete(playlistEntity: PlaylistEntity)

    @Insert
    fun insertMediaItemPlaylist(vararg mediaItemPlayListAssignment: MediaItemPlayListAssignment)

    @Transaction
    fun setPlaylist(mediaItemId: String, playlistId: Long) {
        val mediaItemPlaylist = MediaItemPlayListAssignment(
            mediaItemId,
            playlistId,
            getAvailablePosition(playlistId) ?: 0
        )

        insertMediaItemPlaylist(mediaItemPlaylist)
    }


    @Query("SELECT MAX(position) + 1 from media_item_playlist_assignment WHERE playlistId = :playlistId")
    fun getAvailablePosition(playlistId: Long): Int?

    @Transaction
    suspend fun detachPlaylist(mediaItemId: String, playlistId: Long) {
        mDecreasePositionsInPlaylists(mediaItemId, playlistId)
        mRemovePlaylistAssignment(mediaItemId, playlistId)
    }

    @Transaction
    suspend fun removeMediaItemFromPlaylists(mediaItemId: String) {
        getAssignedPlaylists(mediaItemId).forEach {
            mDecreasePositionsInPlaylists(mediaItemId, it.playlistId)
        }
        mRemoveAllAssignments(mediaItemId)
    }

    @Query(
        """
        SELECT 
            media_items.mediaItemId, 
            media_items.title, 
            media_items.author, 
            media_items.duration, 
            media_items.thumbnail, 
            media_items.mediaFile,
            media_item_playlist_assignment.position,
            media_items.thumbnailUrl
        FROM media_item_playlist_assignment 
        INNER JOIN media_items 
        ON media_items.mediaItemId = media_item_playlist_assignment.mediaItemId
        WHERE media_item_playlist_assignment.playlistId = :playlistId AND media_items.downloadingJobId IS NULL
        ORDER BY media_item_playlist_assignment.position ASC
        """
    )
    fun getMediaItemsForPlaylistFlow(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query(
        """
            SELECT  playlists.playlistId, playlists.name
            FROM media_item_playlist_assignment
            INNER JOIN playlists ON media_item_playlist_assignment.playlistId = playlists.playlistId
            WHERE media_item_playlist_assignment.mediaItemId = :mediaItemId
        """
    )
    suspend fun getAssignedPlaylists(mediaItemId: String): List<PlaylistEntity>

    @Transaction
    suspend fun changePosition(mediaItemId: String, playlistId: Long, from: Int, to: Int) {
        if (from > to)
            mIncreasePositionInRange(playlistId, from, to)
        else
            mDecreasePositionInRange(playlistId, from, to)

        mSetPosition(mediaItemId, playlistId, to)
    }

    @Query("DELETE FROM media_item_playlist_assignment WHERE playlistId = :playlistId")
    suspend fun removePlaylistAssignments(playlistId: Long)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = :position
        WHERE mediaItemId = :mediaItemId AND playlistId = :playlistId
         """
    )
    suspend fun mSetPosition(mediaItemId: String, playlistId: Long, position: Int)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = position - 1 
        WHERE position > :from AND position <= :to AND playlistId = :playlistId
        """
    )
    suspend fun mDecreasePositionInRange(playlistId: Long, from: Int, to: Int)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = position + 1 
        WHERE position < :from AND position >= :to AND playlistId = :playlistId
    """
    )
    suspend fun mIncreasePositionInRange(playlistId: Long, from: Int, to: Int)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = position - 1 WHERE playlistId = :playlistId AND position > 
        (SELECT position FROM media_item_playlist_assignment WHERE mediaItemId = :mediaItemId AND playlistId = :playlistId)
    """
    )
    fun mDecreasePositionsInPlaylists(mediaItemId: String, playlistId: Long)

    @Query("DELETE FROM media_item_playlist_assignment WHERE mediaItemId = :mediaItemId")
    fun mRemoveAllAssignments(mediaItemId: String)

    @Query("DELETE FROM media_item_playlist_assignment WHERE mediaItemId = :mediaItemId AND playlistId = :playlistId")
    fun mRemovePlaylistAssignment(mediaItemId: String, playlistId: Long)
}