package com.yurii.youtubemusic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.SelectCategoriesDialogBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences

private typealias OnApplyCallBack = (categories: List<Category>) -> Unit

class SelectCategoriesDialog : DialogFragment() {
    private lateinit var binding: SelectCategoriesDialogBinding
    private lateinit var listAdapter: CategoriesAdapter
    private lateinit var onApplyCallBack: OnApplyCallBack

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.select_categories_dialog, container, false)
        listAdapter = CategoriesAdapter(requireContext(), getCategories())
        binding.apply.setOnClickListener {
            onApplyCallBack.invoke(listAdapter.selectedCategories)
            dismiss()
        }
        initListView()
        return binding.root
    }

    private fun initListView() {
        binding.categories.apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            this.adapter = listAdapter
        }
    }

    private fun getCategories(): List<Category> = Preferences.getMusicCategories(requireContext())

    companion object {
        fun create(callBack: OnApplyCallBack) = SelectCategoriesDialog().also {
            it.onApplyCallBack = callBack
        }
    }
}

private class CategoriesAdapter(context: Context, private val categories: List<Category>) : BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(context)
    val selectedCategories = mutableListOf<Category>()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: CheckedTextView = layoutInflater.inflate(R.layout.item_choice, parent, false) as CheckedTextView
        view.text = categories[position].name
        view.setOnClickListener {
            (it as CheckedTextView).toggle()
            categories[position].run {
                if (selectedCategories.contains(this))
                    selectedCategories.remove(this)
                else
                    selectedCategories.add(this)
            }
        }
        return view
    }

    override fun getItem(position: Int): Category = categories[position]

    override fun getItemId(position: Int): Long = categories[position].id.toLong()

    override fun getCount(): Int = categories.size
}