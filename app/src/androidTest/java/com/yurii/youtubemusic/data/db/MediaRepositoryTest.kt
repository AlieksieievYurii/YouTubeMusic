package com.yurii.youtubemusic.data.db

import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.yurii.youtubemusic.db.DataBase
import com.yurii.youtubemusic.services.media.MediaStorage
import com.yurii.youtubemusic.source.MediaRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import com.yurii.youtubemusic.db.MediaItemDao

@RunWith(AndroidJUnit4::class)
@SmallTest
class MediaRepositoryTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var mediaItemDao: MediaItemDao

    @Before
    fun createDataBase() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        val mediaStorageMock = mockk<MediaStorage>()
        mediaItemDao = database.mediaItemDao()
        mediaRepository = MediaRepository(mediaItemDao, mediaStorageMock)
    }

    @Test
    fun test_deleteItem_itemDeletedAndPositionsUpdated() = runBlocking {
        val n = 10
        val mediaItems = createMediaItems(n).onEach { mediaItem -> mediaRepository.addMediaItem(mediaItem) }

        val testedTargetMediaItem = mediaItems[3]
        mediaRepository.delete(testedTargetMediaItem)

        val mediaItemsUpdated = mediaRepository.getOrderedMediaItems().first()
        assertThat(mediaItemsUpdated).hasSize(n - 1)

        val mediaItemsEntities = mediaItemDao.getAllSortedMediaItems().first()
        (0 until n - 1).forEach { assertThat(mediaItemsEntities[it].position).isEqualTo(it) }
    }
}