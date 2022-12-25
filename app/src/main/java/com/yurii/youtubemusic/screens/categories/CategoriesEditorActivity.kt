package com.yurii.youtubemusic.screens.categories

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.viewbinding.library.activity.viewBinding
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ActivityCategoriesEditorBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.AddEditCategoryDialog
import com.yurii.youtubemusic.utilities.Injector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoriesEditorActivity : AppCompatActivity() {
    private val viewModel by viewModels<CategoriesEditorViewModel> { Injector.provideCategoriesEditorViewMode(this) }
    private val binding: ActivityCategoriesEditorBinding by viewBinding()

    private val onDeleteClick = View.OnClickListener {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_playlist_title,
            messageId = R.string.dialog_confirm_deletion_playlist_message,
            onConfirm = { viewModel.removeCategory((it as Chip).id) }
        ).show(supportFragmentManager, "DeleteCategoryDialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActionBar()

        binding.fab.setOnClickListener {
            AddEditCategoryDialog.showToCreate(this) { viewModel.createCategory(it) }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeCategoriesState() }
            }
        }
    }

    private suspend fun observeCategoriesState() {
        viewModel.state.collectLatest {
            when (it) {
                is CategoriesEditorViewModel.State.Loaded ->
                    if (it.categories.isNullOrEmpty()) setNoCategories() else setCategories(it.categories)
                CategoriesEditorViewModel.State.Loading -> {
                    //nothing
                }
            }
        }
    }

    private fun setCategories(categoriesList: List<Category>) {
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

    private fun inflateChip(category: Category): Chip {
        val chip = layoutInflater.inflate(R.layout.category_chip, binding.categories, false) as Chip
        chip.apply {
            id = category.id
            text = category.name
            setOnClickListener {
                AddEditCategoryDialog.showToEdit(this@CategoriesEditorActivity, category) { viewModel.renameCategory(category, it) }
            }
            setOnCloseIconClickListener(onDeleteClick)
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
        onBackPressed()
        return true
    }

    companion object {
        const val REQUEST_CODE = 1000
        fun create(context: Context): Intent {
            return Intent(context, CategoriesEditorActivity::class.java)
        }
    }
}