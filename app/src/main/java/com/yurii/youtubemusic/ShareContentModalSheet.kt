package com.yurii.youtubemusic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.youtubemusic.core.common.toPx
import com.youtubemusic.core.model.MediaItem
import de.hdodenhof.circleimageview.CircleImageView
import net.glxn.qrgen.android.QRCode
import timber.log.Timber

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
        view.findViewById<CardView>(R.id.content).setOnClickListener { openShareSheetToShareURL() }
        view.findViewById<MaterialButton>(R.id.share_file).setOnClickListener { openShareSheetToShareMediaFile() }

        return view
    }

    private fun openShareSheetToShareMediaFile() {
        val fileUri: Uri? = try {
            FileProvider.getUriForFile(requireContext(), requireActivity().packageName, mediaItem.mediaFile)
        } catch (e: IllegalArgumentException) {
            Timber.e("The selected file can't be shared: ${mediaItem.mediaFile}")
            null
        }

        if (fileUri != null) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "audio/*"
            }

            val shareIntent = Intent.createChooser(sendIntent, getString(R.string.label_share_media_file))
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            startActivity(shareIntent)
        }
    }

    private fun openShareSheetToShareURL() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, buildUrl())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.label_share_video_url))
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