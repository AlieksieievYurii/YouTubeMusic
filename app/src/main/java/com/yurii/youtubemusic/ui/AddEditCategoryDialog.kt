package com.yurii.youtubemusic.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.models.Category

private typealias OnApply = (categoryName: String) -> Unit

class AddEditCategoryDialog private constructor() : DialogFragment() {
    private lateinit var edtCategoryName: EditText
    private var onApply: OnApply? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_rename_category, container, false)
        edtCategoryName = view.findViewById(R.id.name)
        val category: Category? = arguments?.getParcelable(EXTRA_CATEGORY)

        view.findViewById<Button>(R.id.on_apply).apply {
            setOnClickListener { onButtonClicked() }
            if (category == null) {
                text = getString(R.string.label_create)
            } else {
                text = getString(R.string.label_rename)
                edtCategoryName.setText(category.name)
            }
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth)
    }

    private fun onButtonClicked() {
        val categoryName = edtCategoryName.text.toString()
        if (TextUtils.isEmpty(categoryName))
            edtCategoryName.error = requireContext().getString(R.string.label_playlist_name_cannot_be_empty)
        else {
            onApply?.invoke(categoryName)
            dismiss()
        }
    }

    companion object {
        private const val EXTRA_CATEGORY = "com.yurii.youtubemusic.category.extra"

        fun showToRenameCategory(fragmentManager: FragmentManager, category: Category, onApply: OnApply) {
            val arguments = Bundle().apply { putParcelable(EXTRA_CATEGORY, category) }
            val dialog = AddEditCategoryDialog()
            dialog.arguments = arguments
            dialog.onApply = onApply

            dialog.show(fragmentManager, "DialogToRenameCategory")
        }

        fun showToCreateCategory(fragmentManager: FragmentManager, onApply: OnApply) {
            val dialog = AddEditCategoryDialog()
            dialog.onApply = onApply

            dialog.show(fragmentManager, "DialogToCreateCategory")
        }
    }
}