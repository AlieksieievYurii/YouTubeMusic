package com.yurii.youtubemusic.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import android.widget.FrameLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior


internal class BottomNavigationBehavior(context: Context, attr: AttributeSet) : HideBottomViewOnScrollBehavior<BottomNavigationView>(context, attr) {
    var enableCollapsingBehavior = true

    override fun layoutDependsOn(parent: CoordinatorLayout, child: BottomNavigationView, dependency: View): Boolean {
        return dependency is FrameLayout
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: BottomNavigationView,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int
    ): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL && enableCollapsingBehavior
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: BottomNavigationView,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (dy > 0)
            slideDown(child)
        else if (dy < 0)
            slideUp(child)
    }
}