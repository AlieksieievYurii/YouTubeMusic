package com.yurii.youtubemusic.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.yurii.youtubemusic.R

class ConfirmDeletionDialog private constructor() : DialogFragment() {
    private lateinit var onConfirm: () -> Unit
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.dialog_confirm_deletion_title)
            builder.setMessage(R.string.dialog_confirm_deletion_message)
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
        fun create(onConfirm: () -> Unit): ConfirmDeletionDialog {
            return ConfirmDeletionDialog().apply {
                this.onConfirm = onConfirm
            }
        }
    }

}