package com.yurii.youtubemusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.yurii.youtubemusic.db.DataBase
import com.yurii.youtubemusic.source.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import com.yurii.youtubemusic.db.MediaItemDao
import java.util.*

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

        mediaItemDao = database.mediaItemDao()
        mediaRepository = MediaRepository(mediaItemDao)
    }

    @Test
    fun test_deleteItem_itemDeletedAndPositionsUpdated() = runBlocking {
        val n = 10
        val mediaItems = createMediaItems(n).onEach { mediaItem -> mediaRepository.addDownloadingMediaItem(mediaItem, UUID(1L,1L), "") }
        mediaItems.forEach { mediaRepository.setMediaItemAsDownloaded(it.id) }
        val testedTargetMediaItem = mediaItems[3]
        mediaRepository.delete(testedTargetMediaItem)

        val mediaItemsUpdated = mediaRepository.getOrderedMediaItems().first()
        assertThat(mediaItemsUpdated).hasSize(n - 1)

        val mediaItemsEntities = mediaItemDao.getAllSortedMediaItems().first()
        (0 until n - 1).forEach { assertThat(mediaItemsEntities[it].position).isEqualTo(it) }
    }
}