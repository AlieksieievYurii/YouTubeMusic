package com.yurii.youtubemusic.utilities

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.yurii.youtubemusic.R

abstract class TabFragment<T: ViewDataBinding>(private val layoutId: Int,
                                               private val titleStringId: Int,
                                               private val optionMenuId: Int? = null) : Fragment() {
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
        toolbar.title = requireContext().getString(titleStringId)
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
}