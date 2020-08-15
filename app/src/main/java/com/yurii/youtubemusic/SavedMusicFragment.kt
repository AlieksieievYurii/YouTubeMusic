package com.yurii.youtubemusic


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yurii.youtubemusic.ui.DownloadButton
import kotlinx.android.synthetic.main.content_main.*

/**
 * A simple [Fragment] subclass.
 */
class SavedMusicFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val toolbar = (activity as AppCompatActivity).toolbar
        toolbar.menu.clear()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_saved_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnDownload = view.findViewById<DownloadButton>(R.id.btn_download)
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


}
