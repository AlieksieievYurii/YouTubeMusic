package com.yurii.youtubemusic.screens.categories

import androidx.lifecycle.*
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.services.media.MediaLibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class CategoriesEditorViewModel(private val mediaLibraryManager: MediaLibraryManager) : ViewModel() {
    sealed class State {
        object Loading : State()
        data class Loaded(val categories: List<Category>) : State()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state.asStateFlow()

    private val reservedIds = mutableListOf<Int>()

    init {
        viewModelScope.launch {
            _state.value = State.Loaded(mediaLibraryManager.mediaStorage.getCustomCategories().also { categories ->
                reservedIds.addAll(categories.map { it.id })
            })
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            mediaLibraryManager.renameCategory(category, newName)
        }
    }

    fun removeCategory(categoryId: Int) {
        viewModelScope.launch {
            val category = mediaLibraryManager.mediaStorage.getCategoryContainer(categoryId).category
            reservedIds.remove(categoryId)
            _state.value = State.Loaded(getCurrentLoadedMutableCategories().apply { remove(category) })
            mediaLibraryManager.removeCategory(category)
        }
    }

    fun createCategory(name: String) {
        val category = Category(generateId(), name)
        _state.value = State.Loaded(getCurrentLoadedMutableCategories().apply { add(category) })
        viewModelScope.launch {
            mediaLibraryManager.createCategory(category)
        }
    }

    private fun getCurrentLoadedMutableCategories(): MutableList<Category> {
        return (_state.value as? State.Loaded)?.categories?.toMutableList()
            ?: throw IllegalStateException("Can not createCategory while it is not loaded")
    }

    private fun generateId(): Int {
        var id = 1
        while (reservedIds.contains(id))
            id++
        reservedIds.add(id)
        return id
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mediaLibraryManager: MediaLibraryManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriesEditorViewModel::class.java))
                return CategoriesEditorViewModel(mediaLibraryManager) as T
            throw IllegalStateException("Given the model class is not assignable from CategoriesEditorViewModel class")
        }
    }
}

