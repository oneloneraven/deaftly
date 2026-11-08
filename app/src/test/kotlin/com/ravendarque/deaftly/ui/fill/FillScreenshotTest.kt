package com.ravendarque.deaftly.ui.fill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.components.SheetBody
import com.ravendarque.deaftly.ui.components.SheetHandle
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import com.ravendarque.deaftly.ui.theme.Deaftly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.fill"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class FillScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun shot() {
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun screen(
        templateId: String,
        dark: Boolean,
        interact: () -> Unit = {},
    ) {
        val viewModel = FillViewModel(templateId, FakeTemplateRepository(fillTemplates), SavedStateHandle())
        composeRule.setContent {
            AppTheme(darkTheme = dark) { FillScreen(templateId, viewModel, onNavigate = {}, onBack = {}) }
        }
        composeRule.waitForIdle()
        interact()
        composeRule.waitForIdle()
        shot()
    }

    private fun sheet(
        templateId: String,
        placeholderKey: String,
        dark: Boolean,
        value: String? = null,
    ) {
        val placeholder = fillTemplates.first { it.id == templateId }.placeholder(placeholderKey)
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                SheetPreview(ActivePlaceholder(placeholder, listOfNotNull(value)))
            }
        }
        composeRule.waitForIdle()
        shot()
    }

    @Composable
    private fun SheetPreview(active: ActivePlaceholder) {
        Surface(color = Deaftly.colors.surface, shape = Deaftly.shapes.sheet) {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
                SheetBody(active.placeholder.label) { FillSheetContent(active, onChoose = {}, onScan = {}) }
            }
        }
    }

    private fun openReplyChip() {
        composeRule.onNodeWithText("how to reply").performClick()
    }

    @Test
    fun previewLightTheme() = screen("check-in", dark = false)

    @Test
    fun previewDarkTheme() = screen("check-in", dark = true)

    @Test
    @Config(fontScale = 2f)
    fun previewLargeFontScale() = screen("check-in", dark = false)

    @Test
    fun previewWithAChipFocused() = screen("check-in", dark = false, interact = ::openReplyChip)

    @Test
    fun sheetLightTheme() = sheet("check-in", "how to reply", dark = false, value = "type on my phone")

    @Test
    fun sheetDarkTheme() = sheet("check-in", "how to reply", dark = true, value = "type on my phone")

    @Test
    @Config(fontScale = 2f)
    fun sheetLargeFontScale() = sheet("check-in", "how to reply", dark = false, value = "type on my phone")

    @Test
    fun cameraSheetLightTheme() = sheet("order-food", "menu item", dark = false)

    @Test
    fun typingSheetLightTheme() = sheet("check-in", "name", dark = false)

    private fun orderWithTwoItems() {
        listOf("Pasta arrabbiata", "Garlic bread").forEachIndexed { index, item ->
            composeRule.onNodeWithText(if (index == 0) "menu item" else "add more").performClick()
            composeRule.onNodeWithText("Type it").performClick()
            composeRule.onNode(hasSetTextAction()).performTextInput(item)
            composeRule.onNode(hasSetTextAction()).performImeAction()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun orderFoodWithTwoItemsLightTheme() = screen("order-food", dark = false, interact = ::orderWithTwoItems)

    @Test
    fun orderFoodWithTwoItemsDarkTheme() = screen("order-food", dark = true, interact = ::orderWithTwoItems)

    @Test
    @Config(fontScale = 2f)
    fun orderFoodWithTwoItemsLargeFontScale() = screen("order-food", dark = false, interact = ::orderWithTwoItems)
}
