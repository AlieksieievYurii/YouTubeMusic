package com.yurii.youtubemusic


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.DownloadButton

/**
 * A simple [Fragment] subclass.
 */
class SavedMusicFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_saved_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnDownload = view.findViewById<DownloadButton>(R.id.btn_download)
        btnDownload.setOnClickStateListener(object : DownloadButton.OnClickListener {
            override fun onClick(view: View, callState: Int): Int {
                return when (callState) {
                    DownloadButton.CALL_DOWNLOAD -> DownloadButton.STATE_DOWNLOADING
                    DownloadButton.CALL_CANCEL -> DownloadButton.STATE_DOWNLOADED
                    DownloadButton.CALL_DELETE -> DownloadButton.STATE_DOWNLOAD
                    else -> 0
                }
            }

        })
    }


}
