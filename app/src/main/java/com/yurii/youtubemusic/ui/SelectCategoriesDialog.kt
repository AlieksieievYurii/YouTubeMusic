package com.yurii.youtubemusic.ui


import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences

private typealias OnApplyCallBack = (categories: ArrayList<Category>) -> Unit

class SelectCategoriesDialog private constructor(private val context: Context) {
    private var callBack: OnApplyCallBack? = null
    private var selectedCategories: List<Category>? = null

    private fun create(categories: List<Category>) {
        val selectedCategories = ArrayList<Category>()
        val categoriesNames = categories.map { it.name }

        val checkedItems = getCheckedItems(categoriesNames)?.also { checkedItems ->
            selectedCategories.addAll(categories.filterIndexed { index, _ -> checkedItems[index] })
        }


        MaterialAlertDialogBuilder(context).apply {
            setTitle(R.string.label_categories)
            if (categories.isEmpty()) {
                setView(R.layout.layout_no_categories)
            } else {
                setMultiChoiceItems(categoriesNames.toTypedArray(), checkedItems) { _, which, isChecked ->
                    val category = categories.find { it.name == categoriesNames[which] }!!
                    if (isChecked)
                        selectedCategories.add(category)
                    else
                        selectedCategories.remove(category)
                }
                setPositiveButton(R.string.label_ok) { _, _ -> callBack?.invoke(selectedCategories) }
                setNegativeButton(R.string.label_cancel, null)
            }
        }.show()
    }

    private fun getCheckedItems(categoriesNames: List<String>): BooleanArray? {
        selectedCategories?.run {
            val checks = BooleanArray(categoriesNames.size)
            this.forEach {
                val index = categoriesNames.indexOf(it.name)
                checks[index] = true
            }
            return checks
        }
        return null
    }

    companion object {
        fun selectCategories(context: Context, selectedCategories: List<Category>?, onApplyCallBack: OnApplyCallBack) {
            val categories = Preferences.getMusicCategories(context)
            SelectCategoriesDialog(context).apply {
                callBack = onApplyCallBack
                this.selectedCategories = selectedCategories
            }.create(categories)
        }
    }
}