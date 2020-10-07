package com.yurii.youtubemusic.viewmodels.categorieseditor

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
class CategoriesEditorViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesEditorViewModel::class.java))
            return CategoriesEditorViewModel(application) as T
        throw IllegalStateException("Given the model class is not assignable from CategoriesEditorViewModel class")
    }
}