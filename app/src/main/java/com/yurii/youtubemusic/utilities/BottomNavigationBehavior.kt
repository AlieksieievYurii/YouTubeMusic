package com.yurii.youtubemusic.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationBehavior : CoordinatorLayout.Behavior<BottomNavigationView> {

    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(parent: CoordinatorLayout, child: BottomNavigationView, dependency: View): Boolean {
        return dependency is FrameLayout
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: BottomNavigationView,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean = axes == ViewCompat.SCROLL_AXIS_VERTICAL

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: BottomNavigationView,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyConsumed < 0)
            hideBottomNavigationView(child)
        else if (dyConsumed > 0)
            showBottomNavigationView(child)
    }

    private fun showBottomNavigationView(view: BottomNavigationView) {
        view.animate().translationY(view.height.toFloat()).duration = 300
    }

    private fun hideBottomNavigationView(view: BottomNavigationView) {
        view.animate().translationY(0.0F).duration = 300
    }
}