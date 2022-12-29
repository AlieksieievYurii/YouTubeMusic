package com.yurii.youtubemusic.services.media

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
        assertEquals(listOf(MediaLibraryManager.Event.ItemDeleted(mediaItem)),events)
    }
}