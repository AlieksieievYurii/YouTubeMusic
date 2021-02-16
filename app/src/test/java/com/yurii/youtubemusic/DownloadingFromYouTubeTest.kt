package com.yurii.youtubemusic

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.YoutubeException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DownloadingFromYouTubeTest {

    private lateinit var youtubeDownloader: YoutubeDownloader

    @Before
    fun initYouTubeDownloader() {
        youtubeDownloader = YoutubeDownloader()
    }

    @Test
    fun downloadingMusic_downloadedMusic() {
        val testVideoId = "ETxmCCsMoD0"
        try {
            val result = youtubeDownloader.getVideo(testVideoId)
            assertThat(
                "Something wrong! The requested video's id is mismatch with actual",
                result.details().videoId(), `is`(testVideoId)
            )
        } catch (error: YoutubeException) {
            Assert.fail("Downloading stopped working. YouTube may changed the structure of web. Message: ${error.message}")
        }
    }

}