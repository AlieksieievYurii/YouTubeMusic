package com.yurii.youtubemusic

import androidx.fragment.app.Fragment
import android.view.View
import androidx.databinding.ViewDataBinding
import com.yurii.youtubemusic.databinding.FragmentSavedMusicBinding
import com.yurii.youtubemusic.ui.DownloadButton


/**
 * A simple [Fragment] subclass.
 */
class SavedMusicFragment : TabFragment() {
    override fun getTabParameters(): TabParameters {
        return TabParameters(
            layoutId = R.layout.fragment_saved_music,
            title = context!!.getString(R.string.label_fragment_title_saved_music),
            optionMenuId = R.menu.navigation_menu
        )
    }

    override fun onInflatedView(viewDataBinding: ViewDataBinding) {
        val binding = viewDataBinding as FragmentSavedMusicBinding
        val btnDownload = binding.btnDownload
        btnDownload.setOnClickStateListener(object : DownloadButton.OnClickListener {
            override fun onClick(view: View, currentState: Int) {
                val newState = when (currentState) {
                    DownloadButton.STATE_DOWNLOAD -> DownloadButton.STATE_DOWNLOADING
                    DownloadButton.STATE_DOWNLOADING -> DownloadButton.STATE_DOWNLOADED
                    DownloadButton.STATE_DOWNLOADED -> DownloadButton.STATE_DOWNLOAD
                    else -> throw IllegalStateException("Unknown button's state")
                }
                btnDownload.state = newState
            }
        })
    }

    companion object {
        fun createInstance(): SavedMusicFragment = SavedMusicFragment()
    }
}
