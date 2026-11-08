package com.ravendarque.deaftly.ui.editor

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.editor"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1250dp")
class EditorScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun screen(
        id: String?,
        dark: Boolean,
        interact: (EditorViewModel) -> Unit = {},
    ) {
        val repository = EditorTestRepository(editorTemplates, originals = listOf(builtInOrderFood))
        val viewModel = EditorViewModel(id, repository, SavedStateHandle(), editorStrings, newId = { "new-id" })
        composeRule.setContent { AppTheme(darkTheme = dark) { EditorScreen(viewModel) {} } }
        composeRule.waitForIdle()
        interact(viewModel)
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun openMenuItem(viewModel: EditorViewModel) = viewModel.edit { it.select("menu item") }

    private fun openThing(viewModel: EditorViewModel) = viewModel.edit { it.select("thing") }

    @Test
    fun newTemplateLightTheme() = screen(null, dark = false)

    @Test
    fun newTemplateDarkTheme() = screen(null, dark = true)

    @Test
    @Config(fontScale = 2f, qualifiers = "w390dp-h1500dp")
    fun newTemplateLargeFontScale() = screen(null, dark = false)

    @Test
    fun orderFoodWithSettingsLightTheme() = screen("order-food", dark = false, interact = ::openMenuItem)

    @Test
    fun orderFoodWithSettingsDarkTheme() = screen("order-food", dark = true, interact = ::openMenuItem)

    @Test
    @Config(fontScale = 2f, qualifiers = "w390dp-h1900dp")
    fun orderFoodWithSettingsLargeFontScale() = screen("order-food", dark = false, interact = ::openMenuItem)

    @Test
    @Config(qualifiers = "w390dp-h1700dp")
    fun choicesPlaceholderWithOptionsLightTheme() = screen("my-note", dark = false, interact = ::openThing)

    @Test
    @Config(qualifiers = "w390dp-h1700dp")
    fun choicesPlaceholderWithOptionsDarkTheme() = screen("my-note", dark = true, interact = ::openThing)

    @Test
    fun validationErrorsLightTheme() =
        screen(null, dark = false) {
            composeRule.onNodeWithText("Save").performClick()
        }

    @Test
    fun keyNameClashLightTheme() =
        screen("my-note", dark = false) {
            it.edit { draft -> draft.select("thing").renameSelected("name") }
        }
}
