package com.fangjet.weather.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.fangjet.weather.domain.SavedLocation
import com.fangjet.weather.ui.theme.CatsDogsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val locations = listOf(
        SavedLocation(label = "London, GB", latitude = 51.5, longitude = -0.1),
        SavedLocation(label = "Tokyo, JP", latitude = 35.7, longitude = 139.7),
    )

    private var reordered: Pair<Int, Int>? = null

    private fun setList(locations: List<SavedLocation> = this.locations) {
        composeRule.setContent {
            CatsDogsTheme {
                DraggableLocationList(
                    locations = locations,
                    onReorder = { from, to -> reordered = from to to },
                    onRemove = {},
                )
            }
        }
    }

    private fun row(label: String) = composeRule.onNode(hasText(label, substring = true))

    // What TalkBack does when the user picks an action from the row's actions menu.
    private fun customAction(
        label: String,
        action: String,
    ) {
        val actions = row(label).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle { actions.first { it.label == action }.action() }
    }

    @Test
    fun moveDownAndMoveUpReorder() {
        setList()

        customAction("London, GB", "Move down")
        assertEquals(0 to 1, reordered)

        customAction("Tokyo, JP", "Move up")
        assertEquals(1 to 0, reordered)
    }

    @Test
    fun endRowsOnlyOfferTheMoveThatMakesSense() {
        setList()

        val londonActions = row("London, GB").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Move down"), londonActions.map { it.label })
        val tokyoActions = row("Tokyo, JP").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Move up"), tokyoActions.map { it.label })
    }

    @Test
    fun rowsAreNotTappableAndTheBellMarksTheNotificationCity() {
        setList()

        row("London, GB").assert(hasClickAction().not())
        row("Tokyo, JP").assert(hasClickAction().not())
        composeRule.onNodeWithContentDescription("Notifications").assertIsDisplayed()
    }

    @Test
    fun deleteButtonsNameTheirCity() {
        setList()

        composeRule.onNodeWithContentDescription("Delete London, GB").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete Tokyo, JP").assertIsDisplayed()
    }

    @Test
    fun theLastCityHasNoDeleteButton() {
        setList(locations.take(1))

        composeRule.onNodeWithContentDescription("Delete London, GB").assertDoesNotExist()
    }
}
