package com.yurii.youtubemusic.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category
import java.lang.Exception

private open class CategoryException(message: String) : Exception(message)
private open class EmptyCategoryNameException : Exception("Category cannot be empty")
private class CategoryNameAlreadyExistsException : CategoryException("Category name already exists")


private typealias IsExistCategoryNameCallBack = (categoryName: String) -> Boolean
private typealias OnEditCategoryCallBack = (renamedCategory: Category) -> Unit
private typealias OnCreateCategoryCallBack = (categoryName: String) -> Unit

class AddEditCategoryDialog : DialogFragment() {
    private lateinit var isExistCategoryNameCategory: IsExistCategoryNameCallBack
    private var onEditCategoryCallBack: OnEditCategoryCallBack? = null
    private var onCreateCategoryCallBack: OnCreateCategoryCallBack? = null
    private var currentCategory: Category? = null

    private lateinit var edtCategoryName: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_rename_category, container, false)
        edtCategoryName = view.findViewById(R.id.name)
        currentCategory = arguments?.getParcelable(EXTRA_CATEGORY)

        setButton(view)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth)
    }

    private fun setButton(view: View) {
        view.findViewById<Button>(R.id.on_apply).apply {
            if (currentCategory != null) {
                text = getString(R.string.label_rename)
                edtCategoryName.setText(currentCategory!!.name)
                setOnClickListener { renameCategory() }
            } else {
                text = getString(R.string.label_add)
                setOnClickListener { createNewCategory() }
            }
        }
    }

    private fun createNewCategory() {
        getCategoryName { categoryName ->
            onCreateCategoryCallBack?.invoke(categoryName)
            dismiss()
        }
    }

    private fun renameCategory() {
        getCategoryName { categoryName ->
            val category = Category(id = currentCategory!!.id, name = categoryName)
            onEditCategoryCallBack?.invoke(category)
            dismiss()
        }
    }

    private fun getCategoryName(callBack: (categoryName: String) -> Unit) {
        try {
            val categoryName = getAndValidateCategoryName()
            callBack.invoke(categoryName.trim())
        } catch (error: EmptyCategoryNameException) {
            edtCategoryName.error = requireContext().getString(R.string.label_playlist_name_cannot_be_empty)
        } catch (error: CategoryNameAlreadyExistsException) {
            edtCategoryName.error = requireContext().getString(R.string.label_category_already_exists)
        }
    }

    private fun getAndValidateCategoryName(): String {
        val categoryName = edtCategoryName.text.toString()
        if (TextUtils.isEmpty(categoryName))
            throw EmptyCategoryNameException()
        if (isExistCategoryNameCategory.invoke(categoryName))
            throw CategoryNameAlreadyExistsException()

        return categoryName
    }


    companion object {
        private const val EXTRA_CATEGORY = "com.yurii.youtubemusic.category.extra"

        fun createDialogToEditCategory(
            category: Category,
            isExistCategoryNameCallBack: IsExistCategoryNameCallBack,
            onEditCategoryCallBack: OnEditCategoryCallBack
        ): AddEditCategoryDialog {
            return AddEditCategoryDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_CATEGORY, category)
                }
                this.isExistCategoryNameCategory = isExistCategoryNameCallBack
                this.onEditCategoryCallBack = onEditCategoryCallBack
            }
        }

        fun createDialogToCreateNewCategory(
            isExistCategoryNameCallBack: IsExistCategoryNameCallBack,
            onCreateNewCategoryCallBack: OnCreateCategoryCallBack
        ): AddEditCategoryDialog {
            return AddEditCategoryDialog().apply {
                this.isExistCategoryNameCategory = isExistCategoryNameCallBack
                this.onCreateCategoryCallBack = onCreateNewCategoryCallBack
            }
        }
    }
}