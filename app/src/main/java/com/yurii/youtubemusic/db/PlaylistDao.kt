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
    fun setPlaylist(mediaItemId: String, playlists: List<PlaylistEntity>) {
        removeAllAssignments(mediaItemId)
        val mediaItemPlaylists = playlists.map {
            MediaItemPlayListAssignment(
                mediaItemId,
                it.playlistId,
                getAvailablePosition(it.playlistId) ?: 0
            )
        }

        insertMediaItemPlaylist(*mediaItemPlaylists.toTypedArray())
    }

    @Query("SELECT MAX(position) + 1 from media_item_playlist_assignment WHERE playlistId = :playlistId")
    fun getAvailablePosition(playlistId: Long): Int?

    @Query("DELETE FROM media_item_playlist_assignment WHERE mediaItemId = :mediaItemId")
    fun removeAllAssignments(mediaItemId: String)

    @Query(
        """
        SELECT 
            media_items.mediaItemId, 
            media_items.title, 
            media_items.author, 
            media_items.duration, 
            media_items.thumbnail, 
            media_items.mediaFile,
            media_item_playlist_assignment.position 
        FROM media_item_playlist_assignment 
        INNER JOIN media_items 
        ON media_items.mediaItemId = media_item_playlist_assignment.mediaItemId
        WHERE media_item_playlist_assignment.playlistId = :playlistId
        ORDER BY media_item_playlist_assignment.position ASC
        """
    )
    suspend fun getMediaItemsForPlaylist(playlistId: Long): List<MediaItemEntity>

    @Query(
        """
        SELECT 
            media_items.mediaItemId, 
            media_items.title, 
            media_items.author, 
            media_items.duration, 
            media_items.thumbnail, 
            media_items.mediaFile,
            media_item_playlist_assignment.position 
        FROM media_item_playlist_assignment 
        INNER JOIN media_items 
        ON media_items.mediaItemId = media_item_playlist_assignment.mediaItemId
        WHERE media_item_playlist_assignment.playlistId = :playlistId
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
            increasePositionInRange(playlistId, from, to)
        else
            decreasePositionInRange(playlistId, from, to)

        setPosition(mediaItemId, playlistId, to)
    }

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = :position
        WHERE mediaItemId = :mediaItemId AND playlistId = :playlistId
         """
    )
    suspend fun setPosition(mediaItemId: String, playlistId: Long, position: Int)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = position - 1 
        WHERE position > :from AND position <= :to AND playlistId = :playlistId
        """
    )
    suspend fun decreasePositionInRange(playlistId: Long, from: Int, to: Int)

    @Query(
        """
        UPDATE media_item_playlist_assignment SET position = position + 1 
        WHERE position < :from AND position >= :to AND playlistId = :playlistId
    """
    )
    suspend fun increasePositionInRange(playlistId: Long, from: Int, to: Int)
}