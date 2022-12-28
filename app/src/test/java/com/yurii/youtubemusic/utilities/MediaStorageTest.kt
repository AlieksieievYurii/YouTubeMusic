package com.yurii.youtubemusic.utilities

import android.content.Context
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.services.media.MediaStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MediaStorageTest {

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

    private lateinit var mediaStorage: MediaStorage
    private lateinit var temporaryFolder: TemporaryFolder

    @Before
    fun prepareEnvironment() {
        temporaryFolder = TemporaryFolder().also { it.create() }
        val context = mock<Context> {
            on { filesDir } doReturn temporaryFolder.root
        }
        mediaStorage = MediaStorage(context)
    }

    @Test
    fun createMediaMetadata_successfully_created() {
        mediaStorage.createMediaMetadata(mediaItem)

        val createdMediaMetadataFile = File(temporaryFolder.root, "Metadata\\${mediaItem.id}.json")

        assertTrue("Json file is not created successfully", createdMediaMetadataFile.exists())
        assertTrue(createdMediaMetadataFile.isFile)
    }

    @Test
    fun assignItemToDefaultCategory_categoryDoesNotExist_assigned() = runTest {
        mediaStorage.createMediaMetadata(mediaItem)
        mediaStorage.assignItemToDefaultCategory(mediaItem)

        val defaultCategoryFile = File(temporaryFolder.root, DEFAULT_CATEGORY)

        assertTrue(defaultCategoryFile.exists())
        assertTrue(mediaStorage.getCategoryContainer(Category.ALL).mediaItemsIds.contains(mediaItem.id))
    }

    @Test
    fun assignItemToCustomCategories_successfully_assigned() = runTest {
        mediaStorage.createMediaMetadata(mediaItem)
        mediaStorage.assignItemToDefaultCategory(mediaItem)
        mediaStorage.createMediaMetadata(mediaItemTwo)
        mediaStorage.assignItemToDefaultCategory(mediaItemTwo)

        val categoryOne = Category(1, "one")
        val categoryTwo = Category(2, "two")

        mediaStorage.addCategory(categoryOne)
        mediaStorage.addCategory(categoryTwo)
        mediaStorage.assignItemToCategory(categoryOne, mediaItem)
        mediaStorage.assignItemToCategory(categoryTwo, mediaItem)
        mediaStorage.assignItemToCategory(categoryOne, mediaItemTwo)

        val mediaItemsOfCategoryOne = mediaStorage.getMediaItemsFor(categoryOne)
        val mediaItemsOfCategoryTwo = mediaStorage.getMediaItemsFor(categoryTwo)

        assertTrue(mediaItemsOfCategoryOne.contains(mediaItem))
        assertTrue(mediaItemsOfCategoryTwo.contains(mediaItem))
        assertFalse(mediaItemsOfCategoryTwo.contains(mediaItemTwo))
        assertTrue(mediaItemsOfCategoryOne.contains(mediaItemTwo))
    }

    @Test
    fun getCustomCategoryContainers_successfully_returns_onlyCustomCategories() = runTest {
        val categoryOne = Category(1, "one")
        val categoryTwo = Category(2, "two")
        mediaStorage.addCategory(categoryOne)
        mediaStorage.addCategory(categoryTwo)

        val customCategories = mediaStorage.getCustomCategories()

        assertFalse("getCustomCategories must return only custom categories, not including All", customCategories.contains(Category.ALL))
        assertEquals(listOf(categoryOne, categoryTwo), customCategories)
    }

    @Test
    fun getAllCategories_successfully_returns_allCategories() = runTest {
        val categoryOne = Category(1, "one")
        val categoryTwo = Category(2, "two")
        mediaStorage.addCategory(categoryOne)
        mediaStorage.addCategory(categoryTwo)

        val allCategories = mediaStorage.getAllCategories()

        assertEquals(listOf(Category.ALL, categoryOne, categoryTwo), allCategories)
    }

    @Test
    fun getAssignedCustomCategoriesFor_mediaItemIsAssigned_returnsListOfAssignedCustomCategories() = runTest {
        val categoryOne = Category(1, "one")
        val categoryTwo = Category(2, "two")

        mediaStorage.addCategory(categoryOne)
        mediaStorage.addCategory(categoryTwo)

        mediaStorage.assignItemToCategory(categoryOne, mediaItem)
        mediaStorage.assignItemToCategory(categoryTwo, mediaItem)

        assertEquals(mediaStorage.getAssignedCustomCategoriesFor(mediaItem), listOf(categoryOne, categoryTwo))
    }

    @Test
    fun getAssignedCustomCategoriesFor_noCategoriesAssigned_emptyList() = runTest {
        assertEquals(mediaStorage.getAssignedCustomCategoriesFor(mediaItem), emptyList<Category>())
    }

    @Test
    fun demoteCategory_categoryIsAssigned_categoryDemotedFromMediaItem() = runTest {
        val categoryOne = Category(1, "one")
        val categoryTwo = Category(2, "two")
        mediaStorage.addCategory(categoryOne)
        mediaStorage.addCategory(categoryTwo)
        mediaStorage.assignItemToCategory(categoryOne, mediaItem)
        mediaStorage.assignItemToCategory(categoryTwo, mediaItem)

        mediaStorage.demoteCategory(mediaItem, categoryTwo)

        assertEquals(mediaStorage.getAssignedCustomCategoriesFor(mediaItem), listOf(categoryOne))
    }

    @Test
    fun demoteCategory_defaultCategory_canNotDemoteFromDefaultCategory() = runTest {
        val categoryOne = Category(1, "one")
        mediaStorage.addCategory(categoryOne)
        mediaStorage.assignItemToCategory(categoryOne, mediaItem)

        assertThrows(AssertionError::class.java) {
            runBlocking {
                mediaStorage.demoteCategory(mediaItem, Category.ALL)
            }
        }

        assertEquals(mediaStorage.getAssignedCustomCategoriesFor(mediaItem), listOf(categoryOne))
    }

    companion object {
        private const val CATEGORIES_FOLDER = "Categories"
        private const val DEFAULT_CATEGORY = "$CATEGORIES_FOLDER/0.json"
    }
}