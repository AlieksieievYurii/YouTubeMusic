package com.yurii.youtubemusic.utilities

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.yurii.youtubemusic.R

data class TabParameters(val layoutId: Int, val title: String, val optionMenuId: Int? = null, val onClickOption: ((id: Int) -> Unit)? = null)

abstract class TabFragment : Fragment() {
    protected lateinit var toolbar: Toolbar
    private lateinit var tabParameters: TabParameters

    abstract fun onInflatedView(viewDataBinding: ViewDataBinding)

    abstract fun getTabParameters(): TabParameters

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        tabParameters = getTabParameters()
        val viewDataBinding: ViewDataBinding = DataBindingUtil.inflate(inflater, tabParameters.layoutId, container, false)
        initToolBar()
        onInflatedView(viewDataBinding)
        setHasOptionsMenu(true)

        return viewDataBinding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        setTitle()
        inflateOptionsMenuIfRequired(menu, inflater)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun inflateOptionsMenuIfRequired(menu: Menu, inflater: MenuInflater) {
        tabParameters.optionMenuId?.run { inflater.inflate(this, menu) }
        toolbar.setOnMenuItemClickListener {
            tabParameters.onClickOption?.run { this(it.itemId) }
            true
        }
    }

    private fun setTitle() {
        toolbar.title = tabParameters.title
    }

    private fun initToolBar() {
        toolbar = (activity as AppCompatActivity).findViewById(R.id.toolbar)
    }
}