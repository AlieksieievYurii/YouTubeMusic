package com.yurii.youtubemusic.screens.youtube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R

class LoaderViewHolder : LoadStateAdapter<LoaderViewHolder.LoaderViewHolder>() {
    class LoaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            fun create(viewGroup: ViewGroup): LoaderViewHolder {
                val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_loading, viewGroup, false)
                return LoaderViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: LoaderViewHolder, loadState: LoadState) {
        //nothing
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoaderViewHolder {
        return LoaderViewHolder.create(parent)
    }
}