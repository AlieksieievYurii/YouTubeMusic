package com.yurii.youtubemusic.utilities

import androidx.lifecycle.LiveData
import java.lang.Exception

open class StateLiveData<T> : LiveData<Resource<T>>() {
    protected open fun postLoading(data: T? = null) {
        postValue(Resource.loading(data))
    }

    protected open fun postError(error: Exception) {
        postValue(Resource.error(error))
    }

    protected open fun postSuccess(data: T) {
        postValue(Resource.success(data))
    }
}

class MutableStateLiveData<T> : StateLiveData<T>() {
    public override fun postLoading(data: T?) {
        super.postLoading(data)
    }

    public override fun postError(error: Exception) {
        super.postError(error)
    }

    public override fun postSuccess(data: T) {
        super.postSuccess(data)
    }
}

data class Resource<out T>(val status: Status, val data: T?, val error: Exception?) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }

        fun <T> error(error: Exception?): Resource<T> {
            return Resource(Status.ERROR, null, error)
        }

        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }
    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}