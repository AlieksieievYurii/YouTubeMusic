package com.yurii.youtubemusic.services.media

import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.CategoryContainer
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.VideoItem
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
import java.math.BigInteger


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

    private val videoItem = VideoItem(
        "id",
        "title",
        "author",
        123L,
        "description",
        BigInteger.ONE,
        BigInteger.TEN,
        "http://thumbnail.jpg",
        "http://normal_thumbnail.jpg"
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

    @Test
    fun registerMediaItem_itemDoesNotExistYet_itemRegistered() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<MediaLibraryManager.Event>()
        val existedFileMock = mock<File>()
        whenever(existedFileMock.exists()).thenReturn(true)
        val mediaItemMock = mediaItem.copy(thumbnail = existedFileMock, mediaFile = existedFileMock)

        val job = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        whenever(mediaStorage.getMediaFile(videoItem)).thenReturn(existedFileMock)
        whenever(mediaStorage.getThumbnail(videoItem)).thenReturn(existedFileMock)
        mediaLibraryManager.registerMediaItem(videoItem, emptyList())

        verify(mediaStorage, times(1)).createMediaMetadata(mediaItemMock)
        verify(mediaStorage, times(1)).assignItemToDefaultCategory(mediaItemMock)
        verify(mediaStorage, times(0)).assignItemToCategory(Category.ALL, mediaItemMock)
        assertEquals(listOf(MediaLibraryManager.Event.MediaItemIsAdded(mediaItemMock, emptyList())), events)
        job.cancel()
    }
}