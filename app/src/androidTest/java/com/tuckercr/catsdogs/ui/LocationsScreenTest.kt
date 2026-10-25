package com.tuckercr.catsdogs.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private var activated: Int? = null

    private fun setList(activeIndex: Int = 0) {
        composeRule.setContent {
            CatsDogsTheme {
                DraggableLocationList(
                    locations = locations,
                    activeIndex = activeIndex,
                    onReorder = { from, to -> reordered = from to to },
                    onRemove = {},
                    onSetActive = { activated = it },
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
    fun tappingAnInactiveRowMakesItActive() {
        setList(activeIndex = 0)

        row("Tokyo, JP").performClick()
        assertEquals(1, activated)

        row("London, GB (Active)").assert(hasClickAction().not()).assertIsSelected()
        assertNull(reordered)
    }

    @Test
    fun deleteButtonsNameTheirCity() {
        setList()

        composeRule.onNodeWithContentDescription("Delete London, GB").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete Tokyo, JP").assertIsDisplayed()
    }
}
