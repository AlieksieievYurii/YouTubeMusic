package com.yurii.youtubemusic.dialogplaylists

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.YouTubeService
import com.yurii.youtubemusic.databinding.DialogPlayListsBinding
import java.lang.IllegalStateException

class PlayListsDialogFragment(private val onPlayLists: OnPlayLists) : DialogFragment() {
    interface OnPlayLists {
        fun getPlayLists(onResult: (playLists: List<Playlist>) -> Unit)
    }

    private lateinit var binding: DialogPlayListsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            binding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setPlayLists(playLists: List<Playlist>) {
        if (playLists.isNotEmpty()) {
            val playListsAdapter = PlayListsAdapter(playLists)

            binding.rvPlayLists.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(activity)
                adapter = playListsAdapter
                visibility = View.VISIBLE
            }
        }else
            binding.hintListIsEmpty.visibility = View.VISIBLE

        binding.progressBar.visibility = View.GONE
    }

    fun showPlayLists(fragmentManager: FragmentManager) {
        super.show(fragmentManager, "PlayLists")
        onPlayLists.getPlayLists {
            setPlayLists(it)
        }
    }
}