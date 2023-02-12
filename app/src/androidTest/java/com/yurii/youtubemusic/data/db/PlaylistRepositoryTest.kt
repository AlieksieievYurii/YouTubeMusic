package com.yurii.youtubemusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.yurii.youtubemusic.db.DataBase
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.toMediaItems
import com.yurii.youtubemusic.utilities.PlaylistRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlaylistRepositoryTest {
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var database: DataBase

    @Before
    fun createDataBase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DataBase::class.java)
            .allowMainThreadQueries()
            .build()

        playlistRepository = PlaylistRepository(database.playlistDao())
    }

    @Test
    fun test_createPlaylists_playlistsCreated() = runBlocking {
        val playlistNameOne = "one"
        val playlistNameTwo = "two"
        playlistRepository.addPlaylist(playlistNameOne)
        playlistRepository.addPlaylist(playlistNameTwo)


        val names = playlistRepository.getAllPlaylists().map { it.name }
        assertThat(names).contains(playlistNameOne)
        assertThat(names).contains(playlistNameTwo)
    }

    @Test
    fun test_assignMediaItemToPlaylist_mediaItemIsAssigned() = runBlocking {
        val mediaItemsForPlaylistA = createMediaItemEntities(5, "A").onEach { item -> database.mediaItemDao().insert(item) }
        val mediaItemsForPlaylistB = createMediaItemEntities(10, "B").onEach { item -> database.mediaItemDao().insert(item) }

        val playlistA = MediaItemPlaylist(id = playlistRepository.addPlaylist("A"), "A")
        val playlistB = MediaItemPlaylist(id = playlistRepository.addPlaylist("B"), "B")


        mediaItemsForPlaylistA.forEach {
            playlistRepository.assignMediaItemToPlaylists(it.mediaItemId, listOf(playlistA))
        }

        mediaItemsForPlaylistB.forEach {
            playlistRepository.assignMediaItemToPlaylists(it.mediaItemId, listOf(playlistB))
        }

        val resultMediaItemsForPlaylistA = playlistRepository.getMediaItemsFor(playlistA)
        assertThat(resultMediaItemsForPlaylistA.size).isEqualTo(5)
        assertThat(resultMediaItemsForPlaylistA.first().id).isEqualTo(mediaItemsForPlaylistA.first().mediaItemId)

        val resultMediaItemsForPlaylistB = playlistRepository.getMediaItemsFor(playlistB)
        assertThat(resultMediaItemsForPlaylistB.size).isEqualTo(10)
    }

    @Test
    fun test_changingPositionInPlaylist_positionChanged() = runBlocking {
        val mediaItemsForPlaylistA = createMediaItemEntities(5, "A").onEach { item -> database.mediaItemDao().insert(item) }
        val mediaItemsForPlaylistB = createMediaItemEntities(10, "B").onEach { item -> database.mediaItemDao().insert(item) }

        val playlistA = MediaItemPlaylist(id = playlistRepository.addPlaylist("A"), "A")
        val playlistB = MediaItemPlaylist(id = playlistRepository.addPlaylist("B"), "B")


        mediaItemsForPlaylistA.forEach { playlistRepository.assignMediaItemToPlaylists(it.mediaItemId, listOf(playlistA)) }
        mediaItemsForPlaylistB.forEach { playlistRepository.assignMediaItemToPlaylists(it.mediaItemId, listOf(playlistB)) }

        val mediaItem = mediaItemsForPlaylistA.toMediaItems()[3]
        playlistRepository.changePositionInPlaylist(playlistA, mediaItem, 3, 0)

        val resultMediaItemsForPlaylistA = playlistRepository.getMediaItemsFor(playlistA)
        println(database.playlistDao().getMediaItemsForPlaylist(playlistA.id).map { it.mediaItemId to it.position })
        assertThat(resultMediaItemsForPlaylistA[0].id).isEqualTo(mediaItem.id)
    }

    @Test
    fun test_assignNoPlaylists_playlistsAreNotAssigned() = runBlocking {
        val mediaItem = createMediaItemEntities(1).onEach { item ->
            database.mediaItemDao().insert(item)
        }.first()

        playlistRepository.assignMediaItemToPlaylists(mediaItem.mediaItemId, emptyList())
    }
}