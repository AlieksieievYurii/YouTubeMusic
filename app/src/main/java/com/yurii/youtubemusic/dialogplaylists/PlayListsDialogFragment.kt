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
import com.google.api.services.youtube.model.PlaylistListResponse
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.DialogPlayListsBinding
import com.yurii.youtubemusic.utilities.PaginationListener
import java.lang.IllegalStateException

interface PlayListDialogInterface {
    fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?)
    fun onSelectPlaylist(selectedPlaylist: Playlist)
}

class PlayListsDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogPlayListsBinding
    private var nextPageToken: String? = null
    private var isLoadingNewVideoItems = true
    private lateinit var playListDialogInterface: PlayListDialogInterface

    private val playListsAdapter = PlayListsAdapter(this)

    override fun onClick(v: View) {
        val itemPosition = binding.rvPlayLists.getChildLayoutPosition(v)
        dismiss()
        playListDialogInterface.onSelectPlaylist(playListsAdapter.playLists[itemPosition])
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            binding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
            builder.setView(initView())
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initView(): View {
        val layoutManager = LinearLayoutManager(context)
        binding.rvPlayLists.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean = nextPageToken.isNullOrBlank()

            override fun isLoading(): Boolean = isLoadingNewVideoItems

            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                binding.rvPlayLists.post { playListsAdapter.setLoadingState() }
                playListDialogInterface.loadPlayLists({ playListResponse ->
                    isLoadingNewVideoItems = false
                    playListsAdapter.removeLoadingState()
                    playListsAdapter.addPlayLists(playListResponse.items)
                    this@PlayListsDialogFragment.nextPageToken = playListResponse.nextPageToken
                }, nextPageToken)
            }
        })

        binding.rvPlayLists.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = playListsAdapter
        }

        return binding.root
    }

    fun showPlayLists(fragmentManager: FragmentManager, playListDialogInterface: PlayListDialogInterface) {
        super.show(fragmentManager, "PlayLists")
        this.playListDialogInterface = playListDialogInterface

        playListDialogInterface.loadPlayLists({ playListResponse ->
            isLoadingNewVideoItems = false
            if (playListResponse.items.isEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.hintListIsEmpty.visibility = View.VISIBLE
            } else {
                playListsAdapter.addPlayLists(playListResponse.items)
                this.nextPageToken = playListResponse.nextPageToken
                binding.progressBar.visibility = View.GONE
                binding.rvPlayLists.visibility = View.VISIBLE
            }
        }, null)
    }
}