package com.yurii.youtubemusic.services.media

import android.support.v4.media.session.MediaSessionCompat
import androidx.core.net.toUri
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

@ExperimentalCoroutinesApi
class QueueProviderTest {

    @MockK(relaxUnitFun = true)
    private lateinit var mediaSessionCompat: MediaSessionCompat

    @MockK
    private lateinit var mediaStorage: MediaStorage

    private lateinit var queueProvider: QueueProvider

    @Before
    fun initialization() {
        MockKAnnotations.init(this)
        queueProvider = QueueProvider(mediaSessionCompat, mediaStorage)
        mockkStatic("android.net.Uri")
        every { any<File>().toUri() } returns mockk()
    }

    @Test
    fun createQueueFor_queueIsNotCreatedYet_queueCreated() = runTest {
        val testMediaItems = prepareAndGetMediaItems(5)
        coEvery { mediaStorage.getMediaItemsFor(Category.ALL) }.returns(testMediaItems)

        queueProvider.createQueueFor(Category.ALL)

        assertTrue(queueProvider.isInitialized)
        assertEquals(Category.ALL, queueProvider.currentPlayingCategory)
        assertEquals(testMediaItems.first(), queueProvider.currentQueueItem)

        verify { mediaSessionCompat.setQueue(any()) }
        verify { mediaSessionCompat.setQueueTitle(any()) }

        queueProvider.release()

        assertFalse(queueProvider.isInitialized)
        verify { mediaSessionCompat.setQueue(null) }
        verify { mediaSessionCompat.setQueueTitle(null) }
    }

    @Test
    fun currentCategoryAndQueueItem_queueIsNotInitialized_exceptionThrown() {
        assertThrows(QueueProviderException::class.java) {
            queueProvider.currentQueueItem
        }

        assertThrows(QueueProviderException::class.java) {
            queueProvider.currentPlayingCategory
        }

        assertThrows(QueueProviderException::class.java) {
            queueProvider.changePosition(
                MediaItem(
                    "id",
                    "title",
                    "author",
                    1000L,
                    "description",
                    mockk(), mockk()
                ), 1, 2
            )
        }

        assertThrows(QueueProviderException::class.java) {
            queueProvider.skipToNext()
        }

        assertThrows(QueueProviderException::class.java) {
            queueProvider.skipToPrevious()
        }
    }
    @Test
    fun createQueue_queueAlreadyCreatedForAllCategory_newQueueCreatedForAnotherCategory() = runTest {
        coEvery { mediaStorage.getMediaItemsFor(Category.ALL) }.returns(prepareAndGetMediaItems(5))

        queueProvider.createQueueFor(Category.ALL)

        // ---- Start test ---
        val testMediaItems = prepareAndGetMediaItems(10)
        val testCategory = Category(1, "Custom")
        coEvery { mediaStorage.getMediaItemsFor(testCategory) }.returns(testMediaItems)
        queueProvider.createQueueFor(testCategory)
        assertTrue(queueProvider.isInitialized)
        assertEquals(queueProvider.currentPlayingCategory, testCategory)
        assertEquals(testMediaItems[0], queueProvider.currentQueueItem)
        queueProvider.next()
        assertEquals(testMediaItems[1], queueProvider.currentQueueItem)
        queueProvider.skipToPrevious()
        assertEquals(testMediaItems[0], queueProvider.currentQueueItem)
        queueProvider.skipToPrevious()
        assertEquals(testMediaItems[10], queueProvider.currentQueueItem)
        queueProvider.skipToNext()
        assertEquals(testMediaItems[0], queueProvider.currentQueueItem)
    }

    @Test
    fun changePosition_queueCreated_positionChanged() = runTest {
        val testMediaItems = prepareAndGetMediaItems(5)
        coEvery { mediaStorage.getMediaItemsFor(Category.ALL) }.returns(testMediaItems)
        queueProvider.createQueueFor(Category.ALL)

        val mediaItem = testMediaItems[0]

        queueProvider.changePosition(mediaItem, 0, 3)
        assertEquals(mediaItem, queueProvider.currentQueueItem)
        queueProvider.next()
        assertEquals(testMediaItems[4], queueProvider.currentQueueItem)
    }

    private fun prepareAndGetMediaItems(n: Int): List<MediaItem> {
        return (0..n).map { id ->
            MediaItem(id.toString(), "title-$id", "author-$id", id * 1000L, "description-$id", mockk(), mockk())
        }
    }
}