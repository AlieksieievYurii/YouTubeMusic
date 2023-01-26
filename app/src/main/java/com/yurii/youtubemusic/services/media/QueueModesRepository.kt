package com.yurii.youtubemusic.services.media

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.queueModesStore by preferencesDataStore(name = "queue_modes_store")

@Singleton
class QueueModesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    fun getIsShuffle(): Flow<Boolean> = context.queueModesStore.data.map { it[IS_SHUFFLE] ?: false }

    fun getIsLooped(): Flow<Boolean> = context.queueModesStore.data.map { it[IS_LOOPED] ?: false }

    suspend fun setShuffle(state: Boolean) {
        context.queueModesStore.edit {
            it[IS_SHUFFLE] = state
        }
    }

    suspend fun setLoop(state: Boolean) {
        context.queueModesStore.edit {
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