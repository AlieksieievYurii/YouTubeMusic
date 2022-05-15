package com.yurii.youtubemusic

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.material.chip.Chip
import com.yurii.youtubemusic.data.PreferencesFakeData
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.categories.CategoriesEditorActivity
import com.yurii.youtubemusic.utilities.ServiceLocator
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CategoriesEditorTest {
    private lateinit var preferencesFakeData: PreferencesFakeData

    @Before
    fun initPreferences() {
        preferencesFakeData = PreferencesFakeData()
        ServiceLocator.preferences = preferencesFakeData
    }
    @Test
    fun noCategories_infoAboutNoCategoriesYet() {
        launchActivity<CategoriesEditorActivity>()

        onView(withId(R.id.label_no_categories)).check(matches(isDisplayed()))
        onView(withId(R.id.create)).check(matches(withText(getApplicationContext<Application>().resources.getString(R.string.label_create))))
    }

    @Test
    fun oneCategory_oneCategory() {
        preferencesFakeData.setCategories(listOf(Category(2, "CategoryOne")))
        launchActivity<CategoriesEditorActivity>()
        onView(withId(R.id.label_no_categories)).check(matches(not(isDisplayed())))
        onView(withId(R.id.create)).check(matches(withText(getApplicationContext<Application>().resources.getString(R.string.label_add))))
        chipContainsText("CategoryOne")
    }

    private fun chipContainsText(text: String) {
        onView(allOf(withText(containsString(text)), isAssignableFrom(Chip::class.java))).check(matches(isDisplayed()))
    }

    @Test
    fun createNewCategory_newCategoryDialog() {
        launchActivity<CategoriesEditorActivity>()
        onView(withId(R.id.create)).perform(click())
        onView(withText("Playlist")).check(matches(isDisplayed()))
    }

    @Test
    fun createNewCategory_addNewCategory() {
        val scenario =  launchActivity<CategoriesEditorActivity>()
        onView(withId(R.id.create)).perform(click())
        onView(withId(R.id.name)).perform(typeText("CategoryOne"))
        onView(withId(R.id.on_apply)).perform(click())
        chipContainsText("CategoryOne")
        scenario.moveToState(Lifecycle.State.DESTROYED)
        val result = preferencesFakeData.getMusicCategories()
        assertThat(result, hasSize(1))
        assertThat(result.first().name, `is`("CategoryOne"))
    }

    @Test
    fun fewAlreadyExistingCategories_visibleCategories() {
        preferencesFakeData.setCategories(listOf(Category(2, "One"), Category(3, "Two"), Category(4, "Three")))
        launchActivity<CategoriesEditorActivity>()
        chipContainsText("One")
        chipContainsText("Two")
        chipContainsText("Three")
    }
}