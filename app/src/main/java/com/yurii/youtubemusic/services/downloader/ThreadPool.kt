package com.yurii.youtubemusic.services.downloader

import com.yurii.youtubemusic.models.VideoItem
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadPool {
    private const val KEEP_ALIVE_TIME = 1L
    private val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS
    private var NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    private val decodeWorkQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>()
    private val executionTasks: MutableList<VideoItemTask> = mutableListOf()
    private val decodeThreadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        NUMBER_OF_CORES,
        NUMBER_OF_CORES,
        KEEP_ALIVE_TIME,
        KEEP_ALIVE_TIME_UNIT,
        decodeWorkQueue
    )


    fun execute(task: VideoItemTask) {
        executionTasks.add(task)
        decodeThreadPool.execute(task)
    }

    @Synchronized
    fun cancel(videoItem: VideoItem) {
        findTask(videoItem)?.let {
            it.cancel()
            decodeThreadPool.remove(it)
            executionTasks.remove(it)
        }
    }

    fun findTask(videoItem: VideoItem): VideoItemTask? = executionTasks.find { it.videoItem == videoItem }

    fun completeTask(finishedTask: VideoItemTask) {
        executionTasks.remove(finishedTask)
    }
}
