package com.youtubemusic.core.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.youtubemusic.core.common.R

typealias CallTryAgain = () -> Unit
typealias CallCancel = () -> Unit

class ErrorDialog private constructor() : DialogFragment() {
    private var onTryAgain: CallTryAgain? = null
    private var onCancel: CallCancel? = null
    private var errorMessage: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_error, container, false).also {
            setOnClickListeners(it)
            setErrorMessage(it)
        }
    }

    fun addListeners(onTryAgain: CallTryAgain, onCancel: CallCancel): ErrorDialog = apply {
        this.onTryAgain = onTryAgain
        this.onCancel = onCancel
    }

    private fun setErrorMessage(view: View) {
        view.findViewById<TextView>(R.id.tv_error_message).apply {
            text = this@ErrorDialog.errorMessage
        }
    }
    private fun setOnClickListeners(view: View) {
        setOnTryListener(view)
        setOnCancelListener(view)
    }

    private fun setOnTryListener(view: View) {
        view.findViewById<Button>(R.id.btn_try_again).setOnClickListener {
            onTryAgain?.invoke()
            dismiss()
        }
    }

    private fun setOnCancelListener(view: View) {
        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
    }

    companion object {
        fun create(errorMessage: String): ErrorDialog {
            return ErrorDialog().apply {
                this.errorMessage = errorMessage
            }
        }
    }
}