package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        composeRule.enableAccessibilityChecks()
        composeRule.setContent { AppTheme(darkTheme = false) { Column { content() } } }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun SemanticsNodeInteraction.assertTouchTarget() = assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)

    @Test
    fun buttonsAreLabelledClickableAndMeetTheTouchTarget() {
        var clicks = 0
        show {
            PrimaryButton("Show", onClick = { clicks++ })
            SecondaryButton("Start chat", onClick = { clicks++ })
            InkButton("Type & show", onClick = { clicks++ })
        }

        listOf("Show", "Start chat", "Type & show").forEach { label ->
            composeRule
                .onNodeWithText(label)
                .assert(hasRole(Role.Button))
                .assertHasClickAction()
                .assertHeightIsAtLeast(60.dp)
                .assertTouchTarget()
                .performClick()
        }
        assertEquals(3, clicks)
    }

    @Test
    fun iconButtonExposesItsDescriptionAndMeetsTheTouchTarget() {
        var clicked = false
        show { DeaftlyIconButton(DeaftlyIcons.Settings, "Settings", onClick = { clicked = true }) }

        composeRule
            .onNodeWithContentDescription("Settings")
            .assert(hasRole(Role.Button))
            .assertTouchTarget()
            .performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun disabledButtonDoesNotClick() {
        var clicked = false
        show { PrimaryButton("Show", onClick = { clicked = true }, enabled = false) }

        composeRule.onNodeWithText("Show").performClick()
        assertEquals(false, clicked)
    }

    @Test
    fun chipsReportTheirRoleAndStateAndMeetTheTouchTarget() {
        show {
            PlaceholderChip("empty one", onClick = {}, state = ChipState.Empty)
            PlaceholderChip("filled one", onClick = {}, state = ChipState.Filled)
            PlaceholderChip("focused one", onClick = {}, state = ChipState.Focused)
        }

        composeRule
            .onNodeWithText("empty one")
            .assert(hasRole(Role.Button))
            .assert(hasState("Empty"))
            .assertTouchTarget()
        composeRule
            .onNodeWithText("filled one")
            .assert(hasRole(Role.Button))
            .assert(hasState("Filled in"))
            .assertTouchTarget()
        composeRule
            .onNodeWithText("focused one")
            .assert(hasRole(Role.Button))
            .assert(hasState("Editing"))
            .assertTouchTarget()
    }

    @Test
    fun removableChipHasASeparateLabelledRemoveButton() {
        var removed = 0
        var opened = 0
        show {
            PlaceholderChip(
                label = "Pasta arrabbiata",
                onClick = { opened++ },
                tone = ChipTone.PersonB,
                onRemove = { removed++ },
            )
        }

        composeRule.onNodeWithText("Pasta arrabbiata").performClick()
        composeRule
            .onNodeWithContentDescription("Remove Pasta arrabbiata")
            .assert(hasRole(Role.Button))
            .assertTouchTarget()
            .performClick()
        assertEquals(1, opened)
        assertEquals(1, removed)
    }

    @Test
    fun emptyChipHasNoRemoveButton() {
        show { PlaceholderChip("how to reply", onClick = {}, state = ChipState.Empty, onRemove = {}) }

        composeRule.onNodeWithContentDescription("Remove how to reply").assertDoesNotExist()
    }

    @Test
    fun addMoreChipIsAButton() {
        var clicked = false
        show { AddMoreChip("add more", onClick = { clicked = true }) }

        composeRule
            .onNodeWithText("add more")
            .assert(hasRole(Role.Button))
            .assertTouchTarget()
            .performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun cardsAreButtonsWithTheirTextAvailable() {
        var cardClicks = 0
        show {
            ListCard(
                title = "Explain I'm deaf",
                preview = listOf(PreviewPart.Plain("Please "), PreviewPart.Placeholder("how to reply")),
                onClick = { cardClicks++ },
            )
            PinnedTile("Check in", "Hi! I'm deaf.", DeaftlyIcons.Check, onClick = { cardClicks++ })
        }

        composeRule
            .onNodeWithText("Explain I'm deaf")
            .assert(hasRole(Role.Button))
            .assertTouchTarget()
            .performClick()
        composeRule
            .onNodeWithText("Check in")
            .assert(hasRole(Role.Button))
            .assertTouchTarget()
            .performClick()
        assertEquals(2, cardClicks)
    }

    @Test
    fun topBarHasLabelledBackAndTitleHeading() {
        var back = 0
        show {
            DeaftlyTopBar(
                title = "Order food",
                onNavigate = { back++ },
                trailing = { DeaftlyIconButton(DeaftlyIcons.Edit, "Edit template", onClick = {}) },
            )
        }

        composeRule
            .onNodeWithText("Order food")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("Back").assertTouchTarget().performClick()
        composeRule.onNodeWithContentDescription("Edit template").assertTouchTarget().assertIsDisplayed()
        assertEquals(1, back)
    }

    @Test
    fun dockHoldsItsContentAndRunsItsAction() {
        var clicks = 0
        show {
            BottomDock {
                SecondaryButton("Home", onClick = { clicks++ }, modifier = Modifier.fillMaxWidth())
            }
        }

        composeRule.onNodeWithText("Home").assertTouchTarget().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun hubCardIsALabelledButtonWithAFullTouchTarget() {
        var clicks = 0
        show { HubCard("Start typing", "Enter your own custom text", DeaftlyIcons.Keyboard, onClick = { clicks++ }) }

        composeRule
            .onNodeWithText("Start typing")
            .assertContentDescriptionEquals("Start typing. Enter your own custom text")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertTouchTarget()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun bottomSheetShowsATitleHeadingAndItsContent() {
        var open by mutableStateOf(true)
        show {
            if (open) {
                DeaftlyBottomSheet(title = "How should they reply?", onDismiss = { open = false }) {
                    SecondaryButton("write it down", onClick = {}, modifier = Modifier)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("How should they reply?")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        composeRule.onNodeWithText("write it down").assertIsDisplayed()
    }
}
