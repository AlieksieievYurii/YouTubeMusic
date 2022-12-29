package com.yurii.youtubemusic.services.media

import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.CategoryContainer
import com.yurii.youtubemusic.models.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File


@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MediaLibraryManagerTest {
    private val mediaItem = MediaItem(
        "id",
        "title",
        "author",
        123L,
        "description",
        File("./thumbnail.jpg"),
        File("./media.mp3")
    )

    private val mediaItemTwo = MediaItem(
        "id2",
        "title2",
        "author2",
        123L,
        "description2",
        File("./thumbnail2.jpg"),
        File("./media2.mp3")
    )

    @Mock
    private lateinit var mediaStorage: MediaStorage
    private lateinit var mediaLibraryManager: MediaLibraryManager

    @Before
    fun initialization() {
        mediaStorage = mock()
        mediaLibraryManager = MediaLibraryManager::class.java.getDeclaredConstructor(MediaStorage::class.java).apply {
            isAccessible = true
        }.newInstance(mediaStorage)
    }

    @Test
    fun deleteItem_itemExists_itemDeleted() {
        val events = mutableListOf<MediaLibraryManager.Event>()

        runTest(UnconfinedTestDispatcher()) {
            val job = launch {
                mediaLibraryManager.event.collect { events.add(it) }
            }

            mediaLibraryManager.deleteItem(mediaItem)
            verify(mediaStorage, times(1)).eliminateMediaItem(mediaItem.id)
            job.cancel()
        }
        assertEquals(listOf(MediaLibraryManager.Event.ItemDeleted(mediaItem)), events)
    }

    @Test
    fun changeMediaItemPosition_itemsExist_changed() = runTest(UnconfinedTestDispatcher()) {
        val mockedCategoryContainer = CategoryContainer(Category.ALL, listOf(mediaItem.id, mediaItemTwo.id))
        whenever(mediaStorage.getCategoryContainer(Category.ALL)).thenReturn(mockedCategoryContainer)

        val events = mutableListOf<MediaLibraryManager.Event>()

        val job = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        mediaLibraryManager.changeMediaItemPosition(Category.ALL, mediaItem, 0, 1)
        job.cancel()

        val newCategoryContainer = CategoryContainer(Category.ALL, listOf(mediaItemTwo.id, mediaItem.id)) // Changed order
        verify(mediaStorage, times(1)).saveCategoryContainer(newCategoryContainer)
        assertEquals(listOf(MediaLibraryManager.Event.MediaItemPositionChanged(Category.ALL, mediaItem, 0, 1)), events)
    }
}