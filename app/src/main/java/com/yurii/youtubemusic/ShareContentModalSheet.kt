package com.yurii.youtubemusic

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.ui.toPx
import de.hdodenhof.circleimageview.CircleImageView
import net.glxn.qrgen.android.QRCode

class ShareContentModalSheet : BottomSheetDialogFragment() {
    lateinit var mediaItem: MediaItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sheet_share_content, container, false)

        val quCode = QRCode.from(buildUrl())
            .withSize(view.toPx(250), view.toPx(250))
            .withColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary), 0)
            .bitmap()

        view.findViewById<ImageView>(R.id.qr_code).setImageBitmap(quCode)
        view.findViewById<TextView>(R.id.title).text = mediaItem.title
        view.findViewById<CircleImageView>(R.id.thumbnail).load(mediaItem.thumbnail)
        view.findViewById<CardView>(R.id.content).setOnClickListener { openShareSheet() }

        return view
    }

    private fun openShareSheet() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, buildUrl())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.label_share_video))
        startActivity(shareIntent)
    }
    private fun buildUrl(): String = "${YOUTUBE_WATCH_URL}${mediaItem.id}"

    companion object {
        private const val TAG = "ShareContentModalSheet"
        private const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="

        fun show(mediaItem: MediaItem, fragmentManager: FragmentManager) {
            val instance = ShareContentModalSheet()
            instance.mediaItem = mediaItem
            instance.show(fragmentManager, TAG)
        }
    }
}