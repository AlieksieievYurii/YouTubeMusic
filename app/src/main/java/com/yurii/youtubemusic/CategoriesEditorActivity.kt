package com.yurii.youtubemusic

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.yurii.youtubemusic.databinding.ActivityCategoriesEditorBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.AddEditCategoryDialog
import com.yurii.youtubemusic.utilities.ServiceLocator
import com.yurii.youtubemusic.viewmodels.CategoriesEditorViewModel
import com.yurii.youtubemusic.viewmodels.CategoriesEditorViewModelFactory

class CategoriesEditorActivity : AppCompatActivity() {
    private val viewModel by viewModels<CategoriesEditorViewModel> {
        CategoriesEditorViewModelFactory(ServiceLocator.providePreferences(this))
    }
    private lateinit var binding: ActivityCategoriesEditorBinding

    private val onDeleteClick = View.OnClickListener {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_playlist_title,
            messageId = R.string.dialog_confirm_deletion_playlist_message,
            onConfirm = { removeCategory(it) }
        ).show(supportFragmentManager, "DeleteCategoryDialog")
    }

    private val onEditCategoryName = View.OnClickListener {
        val categoryChip = it as Chip
        val category = viewModel.getCategoryByName(categoryChip.text.toString())

        AddEditCategoryDialog.createDialogToEditCategory(category,
            { categoryName -> viewModel.isCategoryNameExist(categoryName) }) { editedCategory ->
            viewModel.updateCategory(editedCategory)
            categoryChip.text = editedCategory.name
        }.show(supportFragmentManager, "EditCategoryDialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_categories_editor)

        initActionBar()
        initCategoryChips()
    }

    private fun initCategoryChips() {
        viewModel.categories.observe(this, Observer { categories ->
            if (categories.isNullOrEmpty())
                setNoCategories()
            else
                setCategories(categories)
        })
    }

    private fun setCategories(categories: List<Category>) {
        categories.forEach { category -> inflateAndAddChip(category) }
        setShowCategories()
    }

    private fun setShowCategories() {
        binding.labelNoCategories.visibility = View.GONE
        binding.categoriesLayout.visibility = View.VISIBLE
        binding.create.apply {
            text = getString(R.string.label_add)
            setOnClickListener { addNewCategory() }
        }
    }

    private fun setNoCategories() {
        binding.categoriesLayout.visibility = View.GONE
        binding.labelNoCategories.visibility = View.VISIBLE
        binding.create.apply {
            text = getString(R.string.label_create)
            setOnClickListener { createFirstCategory() }
        }
    }

    private fun createFirstCategory() = addNewCategory(isFirstCategory = true)

    private fun addNewCategory(isFirstCategory: Boolean = false) {
        AddEditCategoryDialog.createDialogToCreateNewCategory({ viewModel.isCategoryNameExist(it) }) { categoryName ->
            val category = viewModel.createNewCategory(categoryName)
            inflateAndAddChip(category)
            if (isFirstCategory)
                setShowCategories()
        }.show(supportFragmentManager, "AddCategoryDialog")
    }

    private fun inflateAndAddChip(category: Category) {
        val chip = layoutInflater.inflate(R.layout.category_chip, binding.categories, false) as Chip
        chip.apply {
            text = category.name
            setOnClickListener(onEditCategoryName)
            setOnCloseIconClickListener(onDeleteClick)
        }
        binding.categories.addView(chip)
    }

    private fun initActionBar() {
        supportActionBar?.let {
            it.title = getString(R.string.label_edit_playlists)
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun finish() {
        viewModel.saveChanges()
        setResult(if (viewModel.areChanges) CATEGORIES_ARE_CHANGE_RESULT_CODE else -1)
        super.finish()
    }

    private fun removeCategory(view: View) {
        val category = viewModel.getCategoryByName((view as Chip).text.toString())
        viewModel.removeCategory(category)
        binding.categories.removeView(view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val REQUEST_CODE = 1000
        const val CATEGORIES_ARE_CHANGE_RESULT_CODE = 1
        fun create(context: Context): Intent {
            return Intent(context, CategoriesEditorActivity::class.java)
        }
    }
}