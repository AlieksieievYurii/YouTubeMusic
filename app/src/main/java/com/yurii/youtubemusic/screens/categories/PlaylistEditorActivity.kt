package com.yurii.youtubemusic.screens.categories

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.youtubemusic.core.model.MediaItemPlaylist
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityPlaylistEditorBinding
import com.yurii.youtubemusic.ui.AddEditPlaylistDialog
import com.yurii.youtubemusic.ui.showDeletionDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistEditorActivity : AppCompatActivity() {
    private val viewModel: PlaylistEditorViewModel by viewModels()
    private val binding: ActivityPlaylistEditorBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActionBar()

        binding.fab.setOnClickListener {
            AddEditPlaylistDialog.showToCreate(this) { viewModel.createCategory(it) }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observePlaylists() }
            }
        }
    }

    private suspend fun observePlaylists() {
        viewModel.playlistsFlow.collect {
            if (it.isEmpty())
                setNoCategories()
            else
                setCategories(it)
        }
    }

    private fun setCategories(categoriesList: List<MediaItemPlaylist>) {
        binding.categories.apply {
            removeAllViews()
            categoriesList.forEach { category -> addView(inflateChip(category)) }
        }
        setShowCategories()
    }

    private fun setShowCategories() {
        binding.labelNoCategories.visibility = View.GONE
        binding.categoriesLayout.visibility = View.VISIBLE
    }

    private fun setNoCategories() {
        binding.apply {
            categoriesLayout.visibility = View.GONE
            labelNoCategories.visibility = View.VISIBLE
        }
    }

    private fun inflateChip(playlist: MediaItemPlaylist): Chip {
        val chip = layoutInflater.inflate(R.layout.category_chip, binding.categories, false) as Chip
        chip.apply {
            id = playlist.id.toInt()
            text = playlist.name
            setOnClickListener {
                AddEditPlaylistDialog.showToEdit(this@PlaylistEditorActivity, playlist) { viewModel.renameCategory(playlist, it) }
            }
            setOnCloseIconClickListener {
                showDeletionDialog(
                    this@PlaylistEditorActivity,
                    R.string.dialog_confirm_deletion_playlist_title,
                    R.string.dialog_confirm_deletion_playlist_message
                ) {
                    viewModel.removePlaylist(playlist)
                }
            }
        }
        return chip
    }

    private fun initActionBar() {
        supportActionBar?.apply {
            title = getString(R.string.label_edit_playlists)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}