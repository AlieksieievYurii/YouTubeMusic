package com.yurii.youtubemusic.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R

fun showDeletionDialog(context: Context, titleId: Int, messageId: Int, callback: () -> Unit) {
    MaterialAlertDialogBuilder(context).apply {
        setTitle(titleId)
        setMessage(messageId)
        setPositiveButton(R.string.dialog_delete) { _, _ -> callback.invoke() }
        setNegativeButton(R.string.dialog_cancel, null)
    }.show()
}