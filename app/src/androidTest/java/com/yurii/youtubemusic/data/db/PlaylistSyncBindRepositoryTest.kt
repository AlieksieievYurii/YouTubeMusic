package com.yurii.youtubemusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.yurii.youtubemusic.db.DataBase
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.youtubemusic.core.data.repository.YouTubePlaylistSyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlaylistSyncBindRepositoryTest {
    private lateinit var playlistSyncBindRepository: YouTubePlaylistSyncRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var database: DataBase

    @Before
    fun createDataBase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DataBase::class.java)
            .allowMainThreadQueries()
            .build()

        playlistSyncBindRepository = YouTubePlaylistSyncRepository(database.playlistSyncBindDao())
        playlistRepository = PlaylistRepository(database.playlistDao())
    }

    @Test
    fun test_addYouTubeSync_isAddedSuccessfully() = runBlocking {
        val playlistA = createPlaylist("A")
        val playlistB = createPlaylist("B")
        val playlistC = createPlaylist("C")

        val youTubePlaylistOne = Playlist("One", "One", "https://", 1L)
        val youTubePlaylistTwo =  Playlist("Two", "Two", "https://", 1L)
        val youTubePlaylistThree =  Playlist("Three", "Three", "https://", 1L)

        playlistSyncBindRepository.addYouTubePlaylistSynchronization(youTubePlaylistOne, listOf(playlistA, playlistB, playlistC))
        playlistSyncBindRepository.addYouTubePlaylistSynchronization(youTubePlaylistTwo, listOf(playlistC))
        playlistSyncBindRepository.addYouTubePlaylistSynchronization(youTubePlaylistThree, listOf())

        playlistSyncBindRepository.youTubePlaylistSyncs.first().let { syncList ->
            assertThat(syncList).hasSize(3)
            val itemsFromYouTubePlaylistOne = syncList.find { it.youTubePlaylistId == youTubePlaylistOne.id }?.mediaItemPlaylists
            val itemsFromYouTubePlaylistTwo = syncList.find { it.youTubePlaylistId == youTubePlaylistTwo.id }?.mediaItemPlaylists
            val itemsFromYouTubePlaylistThree = syncList.find { it.youTubePlaylistId == youTubePlaylistThree.id }?.mediaItemPlaylists

            assertThat(itemsFromYouTubePlaylistOne).isNotNull()
            assertThat(itemsFromYouTubePlaylistTwo).isNotNull()
            assertThat(itemsFromYouTubePlaylistThree).isNotNull()
            assertThat(itemsFromYouTubePlaylistOne).containsExactly(playlistA, playlistB, playlistC)
            assertThat(itemsFromYouTubePlaylistTwo).containsExactly(playlistC)
            assertThat(itemsFromYouTubePlaylistThree).isEmpty()
        }
    }

    private suspend fun createPlaylist(name: String): MediaItemPlaylist {
        return MediaItemPlaylist(id = playlistRepository.addPlaylist(name), name)
    }

}