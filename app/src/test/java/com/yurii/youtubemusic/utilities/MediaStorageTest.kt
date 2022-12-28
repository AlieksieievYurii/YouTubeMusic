package com.yurii.youtubemusic.utilities

import android.content.Context
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.services.media.MediaStorage
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class MediaStorageTest {

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
        val mediaItem = MediaItem(
            "id",
            "title",
            "author",
            123L,
            "description",
            File("./thumbnail.jpg"),
            File("./media.mp3")
        )
        mediaStorage.createMediaMetadata(mediaItem)

        val createdMediaMetadataFile = File(temporaryFolder.root, "Metadata\\${mediaItem.id}.json")

        assertTrue("Json file is not created successfully", createdMediaMetadataFile.exists())
        assertTrue(createdMediaMetadataFile.isFile)
    }

}