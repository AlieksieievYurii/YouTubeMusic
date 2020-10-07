package com.yurii.youtubemusic.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.yurii.youtubemusic.R

class ConfirmDeletionDialog : DialogFragment() {
    private lateinit var onConfirm: () -> Unit
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(requireArguments().getInt(EXTRA_TITLE_ID))
            builder.setMessage(requireArguments().getInt(EXTRA_MESSAGE_ID))
            builder.setPositiveButton(R.string.dialog_delete) { _, _ ->
                onConfirm.invoke()
            }

            builder.setNegativeButton(R.string.dialog_cancel) { _, _ ->
                dismiss()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    companion object {
        private const val EXTRA_TITLE_ID = "com.yurii.youtubemusic.title.extra"
        private const val EXTRA_MESSAGE_ID = "com.yurii.youtubemusic.message.extra"

        fun create(titleId: Int, messageId: Int, onConfirm: () -> Unit): ConfirmDeletionDialog {
            return ConfirmDeletionDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_TITLE_ID, titleId)
                    putInt(EXTRA_MESSAGE_ID, messageId)
                }
                this.onConfirm = onConfirm
            }
        }
    }

}