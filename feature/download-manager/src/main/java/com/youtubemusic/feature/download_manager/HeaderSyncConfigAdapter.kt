package com.youtubemusic.feature.download_manager

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.feature.download_manager.databinding.HeaderSyncConfigBinding

class HeaderSyncConfigAdapter(private val callback: Callback) : RecyclerView.Adapter<HeaderSyncConfigAdapter.Header>() {

    interface Callback {
        fun onSyncChange(isEnabled: Boolean)
        fun onAddPlaylistSynchronization()
    }

    var isSyncOn = false
        set(value) {
            if (value != field) {
                field = value
                notifyItemChanged(0)
            }
        }

    var isNoPlaylistSynchronization: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                notifyItemChanged(0)
            }
        }

    private val onEnablePlaylistSync = CompoundButton.OnCheckedChangeListener { _, enabled ->
        callback.onSyncChange(enabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Header {
        val inflater = LayoutInflater.from(parent.context)
        return Header(DataBindingUtil.inflate(inflater, R.layout.header_sync_config, parent, false))
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: Header, position: Int) = holder.onBind()

    inner class Header(private val binding: HeaderSyncConfigBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            binding.apply {
                if (isNoPlaylistSynchronization) {
                    layoutNoPlaylistsSynchronization.isVisible = true
                    enableAutoSync.isVisible = false
                    addPlaylistSynchronization.setOnClickListener { callback.onAddPlaylistSynchronization() }
                } else {
                    layoutNoPlaylistsSynchronization.isVisible = false
                    enableAutoSync.isVisible = true
                    enableAutoSync.apply {
                        setOnCheckedChangeListener(null)
                        isChecked = isSyncOn
                        setOnCheckedChangeListener(onEnablePlaylistSync)
                    }
                }
            }
        }
    }

}