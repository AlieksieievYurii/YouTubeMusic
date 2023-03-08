package com.yurii.youtubemusic.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface MediaItemDao {
    @Insert
    suspend fun insert(mediaItemEntity: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE mediaItemId = :mediaItemId")
    suspend fun getMediaItem(mediaItemId: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE mediaItemId = :mediaItemId AND downloadingJobId IS NOT NULL")
    suspend fun getDownloadingMediaItem(mediaItemId: String): MediaItemEntity?

    @Transaction
    suspend fun deleteAndCorrectPositions(mediaItemId: String) {
        mDecreasePositionFrom(mediaItemId)
        mDeleteById(mediaItemId)
    }

    @Query("SELECT * FROM media_items WHERE downloadingJobId IS NULL ORDER BY position ASC")
    fun getAllSortedMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    fun getMediaItemsEntities(): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET downloadingJobId = NULL WHERE mediaItemId = :mediaItemId")
    suspend fun setMediaItemAsDownloaded(mediaItemId: String)

    @Query("SELECT MAX(position) + 1 from media_items")
    suspend fun getAvailablePosition(): Int?

    @Query("UPDATE media_items SET downloadingJobId = :jobId WHERE mediaItemId = :mediaItemId")
    suspend fun updateDownloadingJobId(mediaItemId: String, jobId: UUID)
    @Transaction
    suspend fun updatePosition(mediaItemId: String, from: Int, to: Int) {
        if (from > to)
            mIncreasePositionInRange(from, to)
        else
            mDecreasePositionInRange(from, to)

        mSetPosition(mediaItemId, to)
    }

    @Query("UPDATE media_items SET position = :position WHERE mediaItemId = :mediaItemId")
    suspend fun mSetPosition(mediaItemId: String, position: Int)

    @Query("UPDATE media_items SET position = position - 1 WHERE position > :from AND position <= :to")
    suspend fun mDecreasePositionInRange(from: Int, to: Int)

    @Query("UPDATE media_items SET position = position + 1 WHERE position < :from AND position >= :to")
    suspend fun mIncreasePositionInRange(from: Int, to: Int)

    @Query("DELETE FROM media_items WHERE mediaItemId = :mediaItemId")
    suspend fun mDeleteById(mediaItemId: String)

    @Query(
        """
        UPDATE media_items SET position = position - 1 WHERE position >
        (SELECT position FROM media_items WHERE mediaItemId = :mediaItemId)
        """
    )
    suspend fun mDecreasePositionFrom(mediaItemId: String)
}