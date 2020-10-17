package com.yurii.youtubemusic.ui


import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences

private typealias OnApplyCallBack = (categories: ArrayList<Category>) -> Unit

class SelectCategoriesDialog private constructor(private val context: Context) {
    private var callBack: OnApplyCallBack? = null

    private fun create(categories: List<Category>) {
        val selectedCategories = ArrayList<Category>()
        val categoriesNames = categories.map { it.name }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.label_categories)
            .setMultiChoiceItems(categoriesNames.toTypedArray(), null) { _, which, isChecked ->
                val category = categories.find { it.name == categoriesNames[which] }!!
                if (isChecked)
                    selectedCategories.add(category)
                else
                    selectedCategories.remove(category)
            }
            .setPositiveButton(R.string.label_ok) { _, _ -> callBack?.invoke(selectedCategories) }
            .setNegativeButton(R.string.label_cancel, null)
            .show()
    }

    companion object {
        fun selectCategories(context: Context, onApplyCallBack: OnApplyCallBack) {
            val categories = Preferences.getMusicCategories(context)
            SelectCategoriesDialog(context).apply {
                callBack = onApplyCallBack
            }.create(categories)
        }
    }
}