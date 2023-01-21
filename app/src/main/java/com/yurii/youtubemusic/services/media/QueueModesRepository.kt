package com.yurii.youtubemusic.services.media

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.queueModesStore by preferencesDataStore(name = "queue_modes_store")

class QueueModesRepository private constructor(private val application: Application) {

    fun getIsShuffle(): Flow<Boolean> = application.queueModesStore.data.map { it[IS_SHUFFLE] ?: false }

    fun getIsLooped(): Flow<Boolean> = application.queueModesStore.data.map { it[IS_LOOPED] ?: false }

    suspend fun setShuffle(state: Boolean) {
        application.queueModesStore.edit {
            it[IS_SHUFFLE] = state
        }
    }

    suspend fun setLoop(state: Boolean) {
        application.queueModesStore.edit {
            it[IS_LOOPED] = state
        }
    }

    companion object {
        private val IS_SHUFFLE = booleanPreferencesKey("is_shuffle")
        private val IS_LOOPED = booleanPreferencesKey("is_looped")

        @Volatile
        private var instance: QueueModesRepository? = null

        fun getInstance(application: Application): QueueModesRepository {
            if (instance == null)
                synchronized(this) {
                    instance = QueueModesRepository(application)
                }

            return instance!!
        }
    }
}