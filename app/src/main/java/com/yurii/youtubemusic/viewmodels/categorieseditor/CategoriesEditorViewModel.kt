package com.yurii.youtubemusic.viewmodels.categorieseditor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Preferences
import java.lang.IllegalStateException
import java.util.*

class CategoriesEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val categoriesList = mutableListOf<Category>()

    private val _categories: MutableLiveData<List<Category>> = MutableLiveData()
    val categories: LiveData<List<Category>> = _categories.also {
        categoriesList.addAll(Preferences.getMusicCategories(application).toMutableList())
        it.postValue(categoriesList)
    }

    var areChanges = false

    fun getCategoryByName(name: String): Category =
        categoriesList.find { it.name == name } ?: throw IllegalStateException("Cannot find category with name $name")

    fun isCategoryNameExist(categoryName: String): Boolean {
        val trimCategoryName = categoryName.toLowerCase(Locale.ROOT).trim()
        if (trimCategoryName == Category.ALL.name)
            return true
        val res = categoriesList.find { it.name.toLowerCase(Locale.ROOT).trim() == trimCategoryName }
        return res != null
    }

    fun updateCategory(category: Category) {
        areChanges = true
        categoriesList.forEachIndexed { index, it ->
            if (category.id == it.id) {
                categoriesList[index] = category
                return
            }
        }
    }

    fun createNewCategory(categoryName: String): Category {
        areChanges = true
        val category = Category(getUniqueCategoryId(), categoryName)
        categoriesList.add(category)
        return category
    }

    private fun getUniqueCategoryId(): Int {
        var id = 1 //Starts from 1 because it assumes that the main category ALL has id 1
        categoriesList.forEach {
            if (it.id > id)
                id = it.id
        }
        return ++id
    }

    fun removeCategory(category: Category) {
        areChanges = true
        categoriesList.remove(category)

        if (categoriesList.isEmpty())
            _categories.postValue(categoriesList)
    }

    fun saveChanges() {
        if (areChanges)
            Preferences.setCategories(getApplication(), categoriesList)
    }
}