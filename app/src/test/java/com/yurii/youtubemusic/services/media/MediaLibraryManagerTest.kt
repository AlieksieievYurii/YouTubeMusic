package com.yurii.youtubemusic.services.media

import android.content.Context
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.CategoryContainer
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.utilities.parentMkdir
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigInteger


@ExperimentalCoroutinesApi
class MediaLibraryManagerTest {
    @MockK(relaxUnitFun = true)
    private lateinit var mediaStorage: MediaStorage

    private lateinit var mediaLibraryManager: MediaLibraryManager

    @Before
    fun initialization() {
        MockKAnnotations.init(this)
        mediaLibraryManager = MediaLibraryManager::class.java.getDeclaredConstructor(MediaStorage::class.java).apply {
            isAccessible = true
        }.newInstance(mediaStorage)
    }

    @Test
    fun deleteItem_itemExists_itemDeleted() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<MediaLibraryManager.Event>()
        val job = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        mediaLibraryManager.deleteItem(mediaItem)

        job.cancel()
        assertEquals(listOf(MediaLibraryManager.Event.ItemDeleted(mediaItem)), events)
        coVerify {
            mediaStorage.eliminateMediaItem(mediaItem.id)
        }
    }

    @Test
    fun changeMediaItemPosition_itemsExist_changed() = runTest(UnconfinedTestDispatcher()) {
        val mockedCategoryContainer = CategoryContainer(Category.ALL, listOf(mediaItem.id, mediaItemTwo.id))
        val events = mutableListOf<MediaLibraryManager.Event>()

        coEvery { mediaStorage.getCategoryContainer(Category.ALL) } returns mockedCategoryContainer

        val job = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        mediaLibraryManager.changeMediaItemPosition(Category.ALL, mediaItem, 0, 1)
        job.cancel()

        val newCategoryContainer = CategoryContainer(Category.ALL, listOf(mediaItemTwo.id, mediaItem.id)) // Changed order

        assertEquals(listOf(MediaLibraryManager.Event.MediaItemPositionChanged(Category.ALL, mediaItem, 0, 1)), events)
        coVerify {
            mediaStorage.saveCategoryContainer(newCategoryContainer)
        }
    }

    @Test
    fun registerMediaItem_itemDoesNotExistYet_itemRegistered() = runTest(UnconfinedTestDispatcher()) {
        val events = mutableListOf<MediaLibraryManager.Event>()
        val existedFileMock = mockk<File> {
            every { exists() } returns true
        }

        val mediaItemMock = mediaItem.copy(thumbnail = existedFileMock, mediaFile = existedFileMock)

        val job = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        every { mediaStorage.getMediaFile(videoItem) } returns existedFileMock
        every { mediaStorage.getThumbnail(videoItem) } returns existedFileMock
        coEvery { mediaStorage.getCustomCategories() } returns emptyList()

        mediaLibraryManager.registerMediaItem(videoItem, emptyList())

        job.cancel()

        coVerify {
            mediaStorage.createMediaMetadata(mediaItemMock)
            mediaStorage.assignItemToDefaultCategory(mediaItemMock)
        }
        assertEquals(listOf(MediaLibraryManager.Event.MediaItemIsAdded(mediaItemMock, emptyList())), events)
        coVerify(exactly = 0) { mediaStorage.assignItemToCategory(Category.ALL, mediaItemMock) }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun registerMediaItem_asynchronously_allItemsRegistered() = runTest(UnconfinedTestDispatcher()) {
        // Initialization
        val temporaryFolder = TemporaryFolder().also { it.create() }
        val events = mutableListOf<MediaLibraryManager.Event>()
        val context = mockk<Context> {
            every { filesDir } returns  temporaryFolder.root
        }
        mediaStorage = MediaStorage(context)
        mediaLibraryManager = MediaLibraryManager::class.java.getDeclaredConstructor(MediaStorage::class.java).apply {
            isAccessible = true
        }.newInstance(mediaStorage)

        val listeningJob = launch {
            mediaLibraryManager.event.collect { events.add(it) }
        }

        // --------------------------

        // Test: register a lot of media items asynchronously
        val mockedVideoItems = initAndGetMockedVideoItems(temporaryFolder.root)
        mockedVideoItems.map {
            async { mediaLibraryManager.registerMediaItem(it, emptyList()) }
        }.awaitAll()


        //  Check if all media items are listed in default category
        val mediaItemsIdsInDefaultCategory = mediaStorage.getCategoryContainer(Category.ALL)
        mockedVideoItems.forEach {
            assertTrue(mediaItemsIdsInDefaultCategory.mediaItemsIds.contains(it.id))
        }

        assertEquals(mockedVideoItems.size, events.size)
        events.forEach { assertTrue(it is MediaLibraryManager.Event.MediaItemIsAdded) }
        listeningJob.cancel()
    }

    private fun initAndGetMockedVideoItems(workspaceFolder: File): List<VideoItem> {
        return (1..100).map { id ->
            File(workspaceFolder, "Musics/$id.mp3").also {
                it.parentMkdir()
                it.writeText("")
            }
            File(workspaceFolder, "Thumbnails/$id.jpeg").also {
                it.parentMkdir()
                it.writeText("")
            }
            VideoItem(
                id.toString(),
                "title-$id",
                "author-$id",
                id * 1000L,
                "description-$id",
                BigInteger.TEN,
                BigInteger.TEN,
                "https://thumbnail-$id.jpeg",
                "https://normal-thumbnail-$id.jpeg"
            )
        }
    }

    companion object {
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
    }
}