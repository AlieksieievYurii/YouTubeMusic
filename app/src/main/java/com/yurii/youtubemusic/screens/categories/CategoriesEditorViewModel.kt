package com.yurii.youtubemusic.screens.categories

import androidx.lifecycle.*
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.IPreferences
import java.lang.IllegalStateException
import java.util.*

class CategoriesEditorViewModel(private val preferences: IPreferences) : ViewModel() {
    private val categoriesList = mutableListOf<Category>()
    private var nextId = -1

    private val _categories: MutableLiveData<List<Category>> = MutableLiveData()
    val categories: LiveData<List<Category>> = _categories.also {
        categoriesList.addAll(preferences.getMusicCategories().toMutableList())
        it.postValue(categoriesList)
        nextId = initLastAvailableId()
    }

    var areChanges = false
        private set

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
        val category = Category(generateId(), categoryName)
        categoriesList.add(category)
        return category
    }

    private fun initLastAvailableId(): Int {
        var id = 1 //Starts from 1 because it assumes that the main category ALL has id 1
        categoriesList.forEach {
            if (it.id > id)
                id = it.id
        }
        return ++id
    }

    private fun generateId(): Int = nextId++

    fun removeCategory(category: Category) {
        areChanges = true
        categoriesList.remove(category)

        if (categoriesList.isEmpty())
            _categories.postValue(categoriesList)
    }

    fun saveChanges() {
        if (areChanges)
            preferences.setCategories(categoriesList)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val preferences: IPreferences): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriesEditorViewModel::class.java))
                return CategoriesEditorViewModel(preferences) as T
            throw IllegalStateException("Given the model class is not assignable from CategoriesEditorViewModel class")
        }
    }
}

