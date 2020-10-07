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
import com.yurii.youtubemusic.databinding.DialogSelectCategoriesBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences

private typealias OnApplyCallBack = (categories: List<Category>) -> Unit

class SelectCategoriesDialog : DialogFragment() {
    private lateinit var binding: DialogSelectCategoriesBinding
    private lateinit var listAdapter: CategoriesAdapter
    private lateinit var onApplyCallBack: OnApplyCallBack
    private lateinit var categories: ArrayList<Category>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_select_categories, container, false)
        categories = ArrayList(Preferences.getMusicCategories(requireContext()))
        listAdapter = CategoriesAdapter(requireContext(), categories)

        initView()
        switchLayouts()
        return binding.root
    }

    private fun initView() {
        binding.apply.setOnClickListener {
            onApplyCallBack.invoke(listAdapter.selectedCategories)
            dismiss()
        }

        binding.categories.apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            this.adapter = listAdapter
        }
    }

    private fun switchLayouts() {
        if (categories.isEmpty()) {
            binding.layoutNoCategories.visibility = View.VISIBLE
            binding.layoutListOfCategories.visibility = View.GONE
        } else {
            binding.layoutNoCategories.visibility = View.GONE
            binding.layoutListOfCategories.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth)
    }

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