package com.yurii.youtubemusic.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.yurii.youtubemusic.db.DataBase
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.media.MediaStorage
import com.yurii.youtubemusic.source.MediaCreator
import com.yurii.youtubemusic.source.MediaRepository
import com.yurii.youtubemusic.source.PlaylistRepository
import com.yurii.youtubemusic.utilities.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
@SmallTest
class MediaRegisterRepositoryTest {
    private lateinit var mediaManagerDomain: MediaCreator
    private lateinit var mediaRepository: MediaRepository
    private lateinit var temporaryFolder: TemporaryFolder

    private lateinit var database: DataBase

    @Before
    fun createDataBase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DataBase::class.java)
            .allowMainThreadQueries()
            .build()

        temporaryFolder = TemporaryFolder().also { it.create() }
        val context = mockk<Context> {
            every { filesDir } returns temporaryFolder.root
        }
        mediaRepository = MediaRepository(database.mediaItemDao(), mockk())
        mediaManagerDomain = MediaCreator(
            mediaRepository, PlaylistRepository(database.playlistDao()), MediaStorage(context)
        )

    }

    @Test
    fun test_registerMediaItem_mediaItemCreated() = runBlocking {
        val videoItem = createVideoItem(1).first()
        mediaManagerDomain.createMediaItem(videoItem, emptyList())

        val mediaItemEntity = database.mediaItemDao().getAllSortedMediaItems().first().first()
        assertThat(mediaItemEntity.mediaItemId).isEqualTo(videoItem.id)
        assertThat(mediaItemEntity.position).isEqualTo(0)
    }

    @Test
    fun test_registerMultipleMediaItems_mediaItemsCreated() = runBlocking {
        val videoItems = createVideoItem(100)
        videoItems.forEach { mediaManagerDomain.createMediaItem(it, emptyList()) }

        val mediaItemEntities = database.mediaItemDao().getAllSortedMediaItems().first()

        assertThat(mediaItemEntities.first().mediaItemId).isEqualTo(videoItems.first().id)
        assertThat(mediaItemEntities.last().mediaItemId).isEqualTo(videoItems.last().id)
        assertThat(mediaItemEntities.last().position).isEqualTo(99)
    }

    @Test
    fun test_registerMultipleMediaItemsAsynchronously_mediaItemsCreated() = runBlocking {
        val videoItems = createVideoItem(100)
        videoItems.map { async { mediaManagerDomain.createMediaItem(it, emptyList()) } }.awaitAll()

        val mediaItemEntities = database.mediaItemDao().getAllSortedMediaItems().first()
        assertThat(mediaItemEntities.last().mediaItemId).isEqualTo(videoItems.last().id)
        assertThat(mediaItemEntities.last().position).isEqualTo(99)
    }

    private fun createVideoItem(n: Int) = (0 until n).map { id ->
        File(temporaryFolder.root, "${THUMBNAIL_FOLDER}/${id}.jpeg").also {
            it.parentMkdir()
            it.writeText("")
        }
        File(temporaryFolder.root, "${MUSIC_FOLDER}/${id}.mp3").also {
            it.parentMkdir()
            it.writeText("")
        }
        VideoItem(
            id = id.toString(),
            title = "title-$id",
            author = "author-$id",
            durationInMillis = id.toLong(),
            description = "description-$id",
            viewCount = BigInteger.valueOf(id.toLong()),
            likeCount = BigInteger.valueOf(id.toLong()),
            thumbnail = "http://thumbnail-$id.json",
            normalThumbnail = "http://thumbnail-normal-$id.json"
        )
    }

    companion object {
        private const val THUMBNAIL_FOLDER = "Thumbnails"
        private const val MUSIC_FOLDER = "Musics"
    }
}