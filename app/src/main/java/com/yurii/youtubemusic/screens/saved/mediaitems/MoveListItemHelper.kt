package com.yurii.youtubemusic.screens.saved.mediaitems

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.models.MediaItem

class MoveListItemHelper(private val callback: (item: MediaItem, from: Int, to: Int) -> Unit) :
    ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    private var selectedItem: MediaItem? = null
    private var pullOutFrom: Int = 0
    private var insertTo: Int = 0

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val adapter = recyclerView.adapter as MediaListAdapter
        val from = viewHolder.absoluteAdapterPosition
        val to = target.absoluteAdapterPosition

        if (selectedItem == null) {
            selectedItem = adapter.currentList[from]
            pullOutFrom = from
        }

        insertTo = to

        adapter.moveItem(from, to)

        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState ==  ACTION_STATE_IDLE) {
            selectedItem?.run { callback.invoke(this, pullOutFrom, insertTo) }
            selectedItem = null
        }else if (actionState == ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.5f
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Ignore
    }
}