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
    }

    @Test
    fun createQueueFor_queueIsNotCreatedYet_queueCreated() = runTest {
        mockkStatic("android.net.Uri")
        every { any<File>().toUri() } returns mockk()
        val testMediaItems = prepareAndGetMediaItems()
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

    private fun prepareAndGetMediaItems(): List<MediaItem> {
        return (0..10).map { id ->
            MediaItem(id.toString(), "title-$id", "author-$id", id * 1000L, "description-$id", mockk(), mockk())
        }
    }
}