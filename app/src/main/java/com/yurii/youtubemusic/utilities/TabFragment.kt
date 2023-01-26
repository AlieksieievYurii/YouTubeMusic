package com.yurii.youtubemusic.utilities

import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.yurii.youtubemusic.R

abstract class TabFragment<T : ViewDataBinding>(
    private val layoutId: Int,
    private val titleStringId: Int,
    private val optionMenuId: Int?
) : Fragment() {
    private lateinit var toolbar: Toolbar
    lateinit var binding: T

    abstract fun onInflatedView(viewDataBinding: T)

    open fun onClickOption(id: Int) {
        // Should be implemented if the tab has options menu
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        initToolBar()
        setHasOptionsMenu(true)

        onInflatedView(binding)
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        changeTitle() // I know this is bad place for that...
        // but currently I do not know where I can catch the moment when the fragment is shown to user.
        // onResume is called only during the initialization!!!

        inflateOptionsMenuIfRequired(menu, inflater)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun inflateOptionsMenuIfRequired(menu: Menu, inflater: MenuInflater) {
        optionMenuId?.run { inflater.inflate(this, menu) }
        toolbar.setOnMenuItemClickListener {
            this.onClickOption(it.itemId)
            true
        }
    }

    private fun initToolBar() {
        toolbar = (activity as AppCompatActivity).findViewById(R.id.toolbar)
    }

    private fun changeTitle() {
        val anim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 200
            repeatCount = 1
            repeatMode = Animation.REVERSE
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    // Nothing
                }

                override fun onAnimationEnd(p0: Animation?) {
                    // Nothing
                }

                override fun onAnimationRepeat(p0: Animation?) {
                    toolbar.title = requireContext().getString(titleStringId)
                }
            })
        }
        toolbar.startAnimation(anim)
    }
}