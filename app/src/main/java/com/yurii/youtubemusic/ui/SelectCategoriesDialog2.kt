package com.yurii.youtubemusic.ui


import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category

class SelectCategoriesDialog2 constructor(
    private val context: Context,
    private val allCategories: List<Category>,
    private val alreadySelectedCategories: List<Category>,
    private val onApplyCallBack: (List<Category>) -> Unit
) {
    private val selectedCategories = ArrayList<Category>()

    fun show() {
        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.label_playlists)
            setChoices(this)
            setPositiveButton(R.string.label_ok) { _, _ -> onApplyCallBack.invoke(selectedCategories) }
            setNegativeButton(R.string.label_cancel, null)
        }.show()
    }

    private fun setChoices(builder: MaterialAlertDialogBuilder) {
        val categoriesNames = allCategories.map { it.name }
        if (categoriesNames.isEmpty()) {
            builder.setView(R.layout.layout_no_categories)
        } else {
            builder.setMultiChoiceItems(categoriesNames.toTypedArray(), getCheckedItems()) { _, which, isChecked ->
                allCategories.find { it.name == categoriesNames[which] }?.also { category ->
                    if (isChecked)
                        selectedCategories.add(category)
                    else
                        selectedCategories.remove(category)
                }
            }
        }
    }

    private fun getCheckedItems(): BooleanArray {
        val checks = BooleanArray(allCategories.size)
        allCategories.forEachIndexed { index, category ->
            if (alreadySelectedCategories.contains(category)) {
                selectedCategories.add(category)
                checks[index] = true
            }
        }

        return checks
    }
}