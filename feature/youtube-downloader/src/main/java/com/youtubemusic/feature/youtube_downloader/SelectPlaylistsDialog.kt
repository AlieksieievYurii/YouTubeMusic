package com.youtubemusic.feature.youtube_downloader


import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.youtubemusic.core.model.MediaItemPlaylist

class SelectPlaylistsDialog constructor(
    private val context: Context,
    private val playlists: List<MediaItemPlaylist>,
    private val alreadySelectedPlaylists: List<MediaItemPlaylist>,
    private val onApplyCallBack: (List<MediaItemPlaylist>) -> Unit
) {
    private val selectedPlaylists = ArrayList<MediaItemPlaylist>()

    fun show() {
        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.label_playlists)
            setChoices(this)
            setPositiveButton(R.string.label_ok) { _, _ -> onApplyCallBack.invoke(selectedPlaylists) }
            setNegativeButton(R.string.label_cancel, null)
        }.show()
    }

    private fun setChoices(builder: MaterialAlertDialogBuilder) {
        val playlistsNames = playlists.map { it.name }
        if (playlistsNames.isEmpty()) {
            builder.setView(R.layout.layout_no_categories)
        } else {
            builder.setMultiChoiceItems(playlistsNames.toTypedArray(), getCheckedItems()) { _, which, isChecked ->
                playlists.find { it.name == playlistsNames[which] }?.also { category ->
                    if (isChecked)
                        selectedPlaylists.add(category)
                    else
                        selectedPlaylists.remove(category)
                }
            }
        }
    }

    private fun getCheckedItems(): BooleanArray {
        val checks = BooleanArray(playlists.size)
        playlists.forEachIndexed { index, category ->
            if (alreadySelectedPlaylists.contains(category)) {
                selectedPlaylists.add(category)
                checks[index] = true
            }
        }

        return checks
    }
}