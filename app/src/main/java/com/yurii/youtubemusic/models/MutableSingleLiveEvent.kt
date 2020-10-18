package com.yurii.youtubemusic.models

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

class MutableSingleLiveEvent<T> : SingleLiveEvent<T>() {

    public override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)
    }

    public override fun removeObservers(owner: LifecycleOwner) {
        super.removeObservers(owner)
    }

    public override fun call() {
        super.call()
    }

    public override fun setValue(value: T?) {
        super.setValue(value)
    }

    fun sendEvent(value: T) {
        super.setValue(value)
    }
}

open class SingleLiveEvent<T> : LiveData<T>() {
    private val observers = CopyOnWriteArraySet<ObserverWrapper<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    @MainThread
    override fun removeObservers(owner: LifecycleOwner) {
        observers.clear()
        super.removeObservers(owner)
    }

    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        observers.remove(observer)
        super.removeObserver(observer)
    }

    @MainThread
    override fun setValue(value: T?) {
        observers.forEach { it.newValue() }
        super.setValue(value)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    protected open fun call() {
        value = null
    }

    private class ObserverWrapper<T>(private val observer: Observer<T>) : Observer<T> {

        private val pending = AtomicBoolean(false)

        override fun onChanged(t: T?) {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }

        fun newValue() {
            pending.set(true)
        }
    }
}