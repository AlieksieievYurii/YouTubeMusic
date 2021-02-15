package com.yurii.youtubemusic

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.preferences.PreferencesFakeData
import com.yurii.youtubemusic.utils.getOrAwaitValue
import com.yurii.youtubemusic.viewmodels.CategoriesEditorViewModel
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesEditorViewModelTest {
    private lateinit var preferencesFakeData: PreferencesFakeData
    private lateinit var viewModel: CategoriesEditorViewModel

    @Before
    fun initViewModel() {
        preferencesFakeData = PreferencesFakeData()
        viewModel = CategoriesEditorViewModel(preferencesFakeData)
    }

    @Test
    fun noCategories() {
        val results: List<Category> = viewModel.categories.getOrAwaitValue()
        assertThat(results, `is`(emptyList()))
    }

    @Test
    fun createNewCategory_oneCategory() {
        viewModel.createNewCategory("Test")
        val results: List<Category> = viewModel.categories.getOrAwaitValue()
        assertThat(results, hasSize(1))
        assertThat("When add new category, variable must be true", viewModel.areChanges, `is`(true))
        results[0].run {
            assertThat(this.name, `is`("Test"))
            assertThat("Must be 2, because it assumes that ALL category has id 1", this.id, `is`(2))
        }
    }

    @Test
    fun addNextCategory_idMustBeIncreased() {
        viewModel.createNewCategory("Test1")
        viewModel.createNewCategory("Test2")
        val results: List<Category> = viewModel.categories.getOrAwaitValue()
        assertThat(results, hasSize(2))
        results.last().run {
            assertThat(this.id, `is`(3))
            assertThat(this.name, `is`("Test2"))
        }
    }
    @Test
    fun notSaveCategories_emptyCategoriesInPreferences() {
        viewModel.createNewCategory("Test1")
        viewModel.createNewCategory("Test2")

        assertThat(preferencesFakeData.getMusicCategories(), empty())

        viewModel.saveChanges()

        assertThat(preferencesFakeData.getMusicCategories(), hasSize(2))
    }
    @Test
    fun removeCategory_categoryMustBeRemoved() {
        viewModel.createNewCategory("Test1")
        viewModel.createNewCategory("Test2")
        viewModel.saveChanges()

        val categoryToRemove = viewModel.categories.getOrAwaitValue().last()

        viewModel.removeCategory(categoryToRemove)

        assertThat(viewModel.categories.getOrAwaitValue(), hasSize(1))
        assertThat(preferencesFakeData.getMusicCategories(), hasSize(1))
        assertThat(preferencesFakeData.getMusicCategories().last().name, `is`("Test1"))
        assertThat(viewModel.areChanges, `is`(true))
    }

    @Test
    fun updateCategory_categoryMustBeUpdated() {
        viewModel.createNewCategory("Test1")
        val category = viewModel.categories.getOrAwaitValue().last()
        val editedCategory = Category(category.id, "Test2")
        viewModel.updateCategory(editedCategory)

        val results: List<Category> = viewModel.categories.getOrAwaitValue()

        assertThat(results, hasSize(1))
        results.last().run {
            assertThat(this.name, `is`("Test2"))
            assertThat(this.id, `is`(2))
        }

    }
    @Test
    fun checkExisting_mustExist() {
        viewModel.createNewCategory("Test1")
        assertThat(viewModel.isCategoryNameExist("Test1"), `is`(true))
    }

    @Test
    fun checkExisting_mustNotExist() {
        viewModel.createNewCategory("Test1")
        assertThat(viewModel.isCategoryNameExist("Test2"), `is`(false))
    }
}