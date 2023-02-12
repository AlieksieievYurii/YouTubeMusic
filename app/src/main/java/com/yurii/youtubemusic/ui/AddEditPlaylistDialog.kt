package com.yurii.youtubemusic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.MediaItemPlaylist

class AddEditPlaylistDialog private constructor(
    private val context: Context,
    private val currentCategoryName: String? = null,
    private val callback: (String) -> Unit
) {
    @SuppressLint("InflateParams")
    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_rename_category, null)
        val inputView = view.findViewById<EditText>(R.id.input)


        val dialog = MaterialAlertDialogBuilder(context).apply {
            setTitle(if (currentCategoryName != null) R.string.label_rename_playlist else R.string.label_create_playlist)
            inputView.hint = context.getString(R.string.label_category_name)
            inputView.setText(currentCategoryName)
            setView(view)
            setPositiveButton(R.string.label_ok, null)
            setNegativeButton(R.string.label_cancel, null)
        }.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val categoryName = inputView.text.toString()
            if (categoryName.trim().isNotEmpty()) {
                dialog.dismiss()
                callback.invoke(categoryName)
            }
            else
                inputView.error = context.getString(R.string.label_playlist_name_cannot_be_empty)
        }

        inputView.requestFocus()
    }

    companion object {
        fun showToCreate(context: Context, callback: (String) -> Unit) =
            AddEditPlaylistDialog(context, null, callback).show()

        fun showToEdit(context: Context, playlist: MediaItemPlaylist, callback: (String) -> Unit) =
            AddEditPlaylistDialog(context, playlist.name, callback).show()
    }
}