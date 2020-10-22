package com.yurii.youtubemusic.ui


import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences

private typealias OnApplyCallBack = (categories: ArrayList<Category>) -> Unit

class SelectCategoriesDialog private constructor(private val context: Context) {
    private var callBack: OnApplyCallBack? = null
    private lateinit var categories: List<Category>
    private var alreadySelectedCategories: List<Category>? = null
    private val selectedCategories = ArrayList<Category>()

    private fun create() {
        val categoriesNames = categories.map { it.name }

        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.label_categories)
            if (categories.isEmpty()) {
                setView(R.layout.layout_no_categories)
            } else {
                setMultiChoiceItems(categoriesNames.toTypedArray(), getCheckedItems()) { _, which, isChecked ->
                    categories.find { it.name == categoriesNames[which] }?.also { category ->
                        if (isChecked)
                            selectedCategories.add(category)
                        else
                            selectedCategories.remove(category)
                    }
                }
                setPositiveButton(R.string.label_ok) { _, _ -> callBack?.invoke(selectedCategories) }
                setNegativeButton(R.string.label_cancel, null)
            }
        }.show()
    }

    private fun getCheckedItems(): BooleanArray? {
        val checks = BooleanArray(categories.size)
        categories.forEachIndexed { index, category ->
            if (alreadySelectedCategories?.contains(category) == true) {
                selectedCategories.add(category)
                checks[index] = true
            }
        }

        return checks
    }

    companion object {
        fun selectCategories(context: Context, alreadySelectedCategories: List<Category>?, onApplyCallBack: OnApplyCallBack) {
            SelectCategoriesDialog(context).apply {
                callBack = onApplyCallBack
                this.categories = Preferences.getMusicCategories(context)
                this.alreadySelectedCategories = alreadySelectedCategories
            }.create()
        }
    }
}