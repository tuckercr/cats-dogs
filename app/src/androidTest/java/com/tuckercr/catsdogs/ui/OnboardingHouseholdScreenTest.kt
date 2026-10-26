package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingHouseholdScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun offersEveryHouseholdAndReportsTheChoice() {
        val chosen = mutableListOf<Household>()
        composeRule.setContent {
            CatsDogsTheme { OnboardingHouseholdScreen(onChosen = { chosen += it }) }
        }

        composeRule.onNodeWithText(string(R.string.household_title)).assertIsDisplayed()
        Household.entries.forEach { household ->
            composeRule.onNodeWithText(string(household.labelRes)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText(string(R.string.household_cat)).performScrollTo().performClick()

        assertEquals(listOf(Household.CAT), chosen)
    }

    @Test
    fun onlyHomesWithADogGetTheWalkCard() {
        assertEquals(
            listOf(Household.DOG, Household.BOTH),
            Household.entries.filter { it.hasDog },
        )
    }
}
