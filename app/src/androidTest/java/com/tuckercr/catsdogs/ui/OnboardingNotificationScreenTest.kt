package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingNotificationScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun showsOnboardingCopyAndBothButtons() {
        composeRule.setContent {
            CatsDogsTheme { OnboardingNotificationScreen(onDone = {}) }
        }

        composeRule.onNodeWithText(string(R.string.notif_onboarding_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.notif_allow_button)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.notif_not_now_button)).assertIsDisplayed()
    }

    /**
     * Only the "Not now" path is exercised: it stays entirely inside the app's own UI. Tapping
     * "Allow notifications" would launch the real POST_NOTIFICATIONS system dialog, which lives
     * outside the Compose hierarchy and would hang or flake the test.
     */
    @Test
    fun tappingNotNowInvokesOnDone() {
        var done = 0
        composeRule.setContent {
            CatsDogsTheme { OnboardingNotificationScreen(onDone = { done++ }) }
        }

        composeRule.onNodeWithText(string(R.string.notif_not_now_button)).performClick()

        assertEquals(1, done)
    }
}
