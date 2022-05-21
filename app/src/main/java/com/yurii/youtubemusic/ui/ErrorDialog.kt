package com.yurii.youtubemusic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import java.lang.Exception

typealias CallTryAgain = (videoItem: VideoItem) -> Unit
typealias CallCancel = (videoItem: VideoItem) -> Unit

class ErrorDialog private constructor() : DialogFragment() {
    private lateinit var videoItem: VideoItem
    private var onTryAgain: CallTryAgain? = null
    private var onCancel: CallCancel? = null
    private var error: Exception? = null

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
            text = this@ErrorDialog.error?.message
        }
    }
    private fun setOnClickListeners(view: View) {
        setOnTryListener(view)
        setOnCancelListener(view)
    }

    private fun setOnTryListener(view: View) {
        view.findViewById<Button>(R.id.btn_try_again).setOnClickListener {
            onTryAgain?.invoke(videoItem)
            dismiss()
        }
    }

    private fun setOnCancelListener(view: View) {
        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            onCancel?.invoke(videoItem)
            dismiss()
        }
    }

    companion object {
        fun create(videoItem: VideoItem, error: Exception?): ErrorDialog {
            return ErrorDialog().apply {
                this.videoItem = videoItem
                this.error = error
            }
        }
    }
}