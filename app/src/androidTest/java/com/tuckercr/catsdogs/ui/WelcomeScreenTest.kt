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

class WelcomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun showsWelcomeCopyAndGetStartedButton() {
        composeRule.setContent {
            CatsDogsTheme { WelcomeScreen(onGetStarted = {}) }
        }

        composeRule.onNodeWithText(string(R.string.welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.welcome_sunny_body)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.welcome_misty_body)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.get_started)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingGetStartedInvokesCallback() {
        var started = 0
        composeRule.setContent {
            CatsDogsTheme { WelcomeScreen(onGetStarted = { started++ }) }
        }

        composeRule.onNodeWithText(string(R.string.get_started)).performScrollTo().performClick()

        assertEquals(1, started)
    }
}
