package com.yurii.youtubemusic.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert
    suspend fun insert(mediaItemEntity: MediaItemEntity)

    @Transaction
    @Query("DELETE FROM media_items WHERE mediaItemId = :mediaItemId")
    suspend fun deleteById(mediaItemId: String)

    @Query("SELECT * FROM media_items ORDER BY position ASC")
    fun getAllSortedMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT MAX(position) + 1 from media_items")
    suspend fun getAvailablePosition(): Int?

    @Query("UPDATE media_items SET position = :position WHERE mediaItemId = :mediaItemId")
    suspend fun setPosition(mediaItemId: String, position: Int)

    @Query("UPDATE media_items SET position = position - 1 WHERE position > :from AND position <= :to")
    suspend fun decreasePositionInRange(from: Int, to: Int)

    @Query("UPDATE media_items SET position = position + 1 WHERE position < :from AND position >= :to")
    suspend fun increasePositionInRange(from: Int, to: Int)

    @Transaction
    suspend fun updatePosition(mediaItemId: String, from: Int, to: Int) {
        if (from > to)
            increasePositionInRange(from, to)
        else
            decreasePositionInRange(from, to)

        setPosition(mediaItemId, to)
    }
}