package com.yurii.youtubemusic.playlists

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
import com.yurii.youtubemusic.services.youtube.ICanceler
import com.yurii.youtubemusic.utilities.PaginationListener
import java.lang.IllegalStateException

interface PlayListsDialogInterface {
    fun loadPlayLists(onLoad: (resp: PlaylistListResponse) -> Unit, nextPageToken: String?): ICanceler
    fun onSelectPlaylist(selectedPlaylist: Playlist)
}

class PlayListsDialogFragment private constructor() : DialogFragment(), OnClickPlayListListener {
    private var nextPageToken: String? = null
    private var isLoadingNewVideoItems = true
    private var currentPlaylist: Playlist? = null
    private var currentRequest: ICanceler? = null
    private lateinit var binding: DialogPlayListsBinding
    private lateinit var playListDialogInterface: PlayListsDialogInterface
    private lateinit var playListsAdapter: PlayListsAdapter


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
        playListsAdapter = PlayListsAdapter(this, currentPlaylist)
        binding.rvPlayLists.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean = nextPageToken.isNullOrBlank()

            override fun isLoading(): Boolean = isLoadingNewVideoItems

            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                binding.rvPlayLists.post { playListsAdapter.setLoadingState() }
                currentRequest = playListDialogInterface.loadPlayLists({ playListsResponse ->
                    addPlayLists(playListsResponse)
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

    private fun addPlayLists(playListsResponse: PlaylistListResponse) {
        isLoadingNewVideoItems = false
        playListsAdapter.removeLoadingState()
        playListsAdapter.addPlayLists(playListsResponse.items)
        nextPageToken = playListsResponse.nextPageToken
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        loadFirstPage()
    }

    private fun loadFirstPage() {
        currentRequest = playListDialogInterface.loadPlayLists({ playListResponse ->
            isLoadingNewVideoItems = false

            if (playListResponse.items.isNotEmpty())
                showPlayLists(playListResponse)
            else
                showHintPlayListIsEmpty()

        }, null)
    }

    private fun showPlayLists(playListsResponse: PlaylistListResponse) {
        playListsAdapter.addPlayLists(playListsResponse.items)
        this.nextPageToken = playListsResponse.nextPageToken
        binding.progressBar.visibility = View.GONE
        binding.rvPlayLists.visibility = View.VISIBLE
    }

    private fun showHintPlayListIsEmpty() {
        binding.progressBar.visibility = View.GONE
        binding.hintListIsEmpty.visibility = View.VISIBLE
    }

    override fun onClickPlayList(playlist: Playlist) {
        if (playlist != currentPlaylist)
            playListDialogInterface.onSelectPlaylist(playlist)

        dismiss()
    }

    override fun onStop() {
        super.onStop()
        currentRequest?.cancel().also {
            currentRequest = null
        }
    }

    companion object {
        fun createDialog(playListsDialogInterface: PlayListsDialogInterface, currentPlayList: Playlist?): PlayListsDialogFragment =
            PlayListsDialogFragment().apply {
                currentPlaylist = currentPlayList
                playListDialogInterface = playListsDialogInterface
            }
    }
}