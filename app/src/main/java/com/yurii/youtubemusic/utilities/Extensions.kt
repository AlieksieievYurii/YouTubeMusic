package com.yurii.youtubemusic.utilities

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.addListener

inline fun getValueAnimator(
    forward: Boolean = true,
    duration: Long,
    interpolator: TimeInterpolator,
    crossinline updateListener: (progress: Float) -> Unit
): ValueAnimator {
    val animator: ValueAnimator = if (forward) ValueAnimator.ofFloat(0f, 1f) else ValueAnimator.ofFloat(1f, 0f)
    animator.addUpdateListener { updateListener(it.animatedValue as Float) }
    animator.duration = duration
    animator.interpolator = interpolator

    return animator
}

inline fun startValueAnimation(start: Float, end: Float, crossinline onEnd: () -> Unit = {}, crossinline update: (value: Float) -> Unit) {
    val valueAnimator = ValueAnimator.ofFloat(start, end)
    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
    valueAnimator.addUpdateListener {
        update.invoke(it.animatedValue as Float)
    }
    valueAnimator.addListener(onEnd = { onEnd.invoke() })
    valueAnimator.start()
}