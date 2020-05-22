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
    private lateinit var mBinding: DialogPlayListsBinding
    private var mNextPageToken: String? = null
    private var mIsLoadingNewVideoItems = true
    private lateinit var mPlayListDialogInterface: PlayListDialogInterface
    private val mPlayListsAdapter = PlayListsAdapter(this)

    override fun onClick(v: View) {
        val itemPosition = mBinding.rvPlayLists.getChildLayoutPosition(v)
        dismiss()
        val selectedPlaylist = mPlayListsAdapter.playLists[itemPosition]
        if (selectedPlaylist != mPlayListsAdapter.currentPlaylist)
            mPlayListDialogInterface.onSelectPlaylist(selectedPlaylist)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
            builder.setView(initView())
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initView(): View {
        val layoutManager = LinearLayoutManager(context)
        mBinding.rvPlayLists.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean = mNextPageToken.isNullOrBlank()

            override fun isLoading(): Boolean = mIsLoadingNewVideoItems

            override fun loadMoreItems() {
                mIsLoadingNewVideoItems = true
                mBinding.rvPlayLists.post { mPlayListsAdapter.setLoadingState() }
                mPlayListDialogInterface.loadPlayLists({ playListResponse ->
                    mIsLoadingNewVideoItems = false
                    mPlayListsAdapter.removeLoadingState()
                    mPlayListsAdapter.addPlayLists(playListResponse.items)
                    this@PlayListsDialogFragment.mNextPageToken = playListResponse.nextPageToken
                }, mNextPageToken)
            }
        })

        mBinding.rvPlayLists.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = mPlayListsAdapter
        }

        return mBinding.root
    }

    fun showPlayLists(fragmentManager: FragmentManager, currentPlayList: Playlist?, playListDialogInterface: PlayListDialogInterface) {
        super.show(fragmentManager, "PlayLists")
        mPlayListsAdapter.currentPlaylist = currentPlayList
        this.mPlayListDialogInterface = playListDialogInterface

        playListDialogInterface.loadPlayLists({ playListResponse ->
            mIsLoadingNewVideoItems = false
            if (playListResponse.items.isEmpty()) {
                mBinding.progressBar.visibility = View.GONE
                mBinding.hintListIsEmpty.visibility = View.VISIBLE
            } else {
                mPlayListsAdapter.addPlayLists(playListResponse.items)
                this.mNextPageToken = playListResponse.nextPageToken
                mBinding.progressBar.visibility = View.GONE
                mBinding.rvPlayLists.visibility = View.VISIBLE
            }
        }, null)
    }
}